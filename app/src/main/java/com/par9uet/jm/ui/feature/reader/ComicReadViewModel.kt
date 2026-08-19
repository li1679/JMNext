package com.par9uet.jm.ui.feature.reader

import android.content.Context
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import com.par9uet.jm.domain.cache.getComicChapterDownloadDir
import com.par9uet.jm.domain.cache.getDownloadDir
import com.par9uet.jm.domain.cache.getLegacyComicChapterDownloadDir
import com.par9uet.jm.domain.cache.listComicImageFiles
import com.par9uet.jm.core.model.Comic
import com.par9uet.jm.core.model.ComicChapter
import com.par9uet.jm.domain.image.ComicPicImageState
import com.par9uet.jm.data.database.model.DownloadComic
import com.par9uet.jm.data.database.dao.DownloadComicDao
import com.par9uet.jm.data.repository.ComicRepository
import com.par9uet.jm.data.network.model.CollectComicResponse
import com.par9uet.jm.data.network.model.ComicDetailResponse
import com.par9uet.jm.data.network.model.ComicPicListResponse
import com.par9uet.jm.data.network.model.NetWorkResult
import com.par9uet.jm.data.storage.LocalSettingManager
import com.par9uet.jm.domain.store.ReadHistoryManager
import com.par9uet.jm.core.common.ToastManager
import com.par9uet.jm.core.model.CommonUIState
import com.par9uet.jm.core.common.log
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream
import kotlin.math.max
import kotlin.math.min

private const val DEFAULT_DECODE_CONCURRENCY = 2
private const val MAX_DECODE_CONCURRENCY = 4

/** 保留窗口在预解码范围之外额外多留的页数，避免在边界来回滚动时反复解码同一页 */
private const val RETAIN_MARGIN = 2

class ComicReadViewModel(
    private val comicRepository: ComicRepository,
    private val picImageLoader: ImageLoader,
    private val localSettingManager: LocalSettingManager,
    private val downloadComicDao: DownloadComicDao,
    private val toastManager: ToastManager,
    private val readHistoryManager: ReadHistoryManager,
) : ViewModel() {
    var isShowToolBar = mutableStateOf(false)
    var currentIndexState = mutableIntStateOf(0)
    var loadedComicId = mutableIntStateOf(-1)
    var readHistoryComicId = mutableIntStateOf(-1)
    private val _comicPicState = MutableStateFlow(
        CommonUIState<List<ComicPicImageState>>(
            isLoading = true
        )
    )
    val comicPicState = _comicPicState.asStateFlow()
    private val _comicDetailState = MutableStateFlow(CommonUIState<Comic>())
    val comicDetailState = _comicDetailState.asStateFlow()
    private val _localChapterList = MutableStateFlow<List<ComicChapter>>(emptyList())
    val localChapterList = _localChapterList.asStateFlow()

    val size: Int get() = _comicPicState.value.data?.size ?: 0

    /** 已解码或正在解码的页码 */
    private val decodedIndices = mutableSetOf<Int>()

    /** 进行中的解码任务，页面被移出保留窗口时据此取消 */
    private val decodeJobs = mutableMapOf<Int, Job>()

    // 信号量始终生效：并发解码若无上限，快速滚动时会同时展开数十张全尺寸位图
    private var decodeSemaphore: Semaphore = Semaphore(DEFAULT_DECODE_CONCURRENCY)
    private var decodeSemaphorePermits: Int = DEFAULT_DECODE_CONCURRENCY

    private fun getDecodeSemaphore(): Semaphore {
        val target = localSettingManager.localSettingState.value
            .readDecodeConcurrency.coerceIn(1, MAX_DECODE_CONCURRENCY)
        if (decodeSemaphorePermits != target) {
            decodeSemaphore = Semaphore(target)
            decodeSemaphorePermits = target
        }
        return decodeSemaphore
    }

    private fun prefetchCount(): Int =
        localSettingManager.localSettingState.value.prefetchCount.coerceAtLeast(0)

    fun getComicDetail(comicId: Int) {
        viewModelScope.launch {
            _comicDetailState.update {
                it.copy(
                    isLoading = true,
                    isError = false,
                    errorMsg = ""
                )
            }
            when (val data = comicRepository.getComicDetail(comicId)) {
                is NetWorkResult.Error -> {
                    _comicDetailState.update {
                        it.copy(
                            isError = true,
                            errorMsg = data.message
                        )
                    }
                }

                is NetWorkResult.Success<ComicDetailResponse> -> {
                    val comic = data.data.toComic()
                    readHistoryComicId.intValue = readHistoryManager.markRead(comic, comicId)
                    _comicDetailState.update {
                        it.copy(
                            data = comic
                        )
                    }
                }
            }
            _comicDetailState.update {
                it.copy(isLoading = false)
            }
        }
    }

    fun clearComicDetail() {
        _comicDetailState.update { CommonUIState() }
    }

    fun collect(comicId: Int) {
        updateCollectState(comicId, true)
    }

    fun unCollect(comicId: Int) {
        updateCollectState(comicId, false)
    }

    private fun updateCollectState(comicId: Int, targetCollect: Boolean) {
        viewModelScope.launch {
            when (val data: NetWorkResult<CollectComicResponse> = if (targetCollect) {
                comicRepository.collectComic(comicId)
            } else {
                comicRepository.unCollectComic(comicId)
            }) {
                is NetWorkResult.Error -> {
                    toastManager.showAsync(data.message)
                }

                is NetWorkResult.Success<CollectComicResponse> -> {
                    toastManager.showAsync(if (targetCollect) "收藏成功" else "取消收藏成功")
                    _comicDetailState.update {
                        it.copy(
                            data = it.data?.copy(isCollect = targetCollect)
                        )
                    }
                }
            }
        }
    }

    fun getComicPicList(comicId: Int, shunt: String, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            _localChapterList.value = emptyList()
            _comicPicState.update {
                it.copy(
                    isLoading = true,
                    isError = false,
                    errorMsg = ""
                )
            }
            releaseAll()
            when (val data = comicRepository.getComicPicList(comicId, shunt)) {
                is NetWorkResult.Error -> {
                    _comicPicState.update {
                        it.copy(
                            isError = true,
                            errorMsg = data.message
                        )
                    }
                }

                is NetWorkResult.Success<ComicPicListResponse> -> {
                    _comicPicState.update {
                        it.copy(
                            data = data.data.list.mapIndexed { index, item ->
                                ComicPicImageState(
                                    index,
                                    comicId,
                                    item,
                                    data.data.__scrambleId,
                                    data.data.__speed,
                                    picImageLoader,
                                    imageFetcher = {
                                        comicRepository.downloadImageBytes(comicId, index)
                                    },
                                    __aId = data.data.__aId
                                )
                            }
                        )
                    }
                    onSuccess?.invoke()
                }
            }
            _comicPicState.update {
                it.copy(
                    isLoading = false
                )
            }
        }
    }

    fun getLocalComicPicList(comicId: Int, context: Context, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            _comicPicState.update {
                it.copy(
                    isLoading = true,
                    isError = false,
                    errorMsg = ""
                )
            }
            releaseAll()
            val downloadComic = downloadComicDao.getById(comicId)
            val groupId = downloadComic?.groupId?.takeIf { it != 0 } ?: comicId
            readHistoryComicId.intValue = readHistoryManager.markRead(groupId, comicId)
            loadLocalChapterList(comicId, downloadComic)
            val imageDir = ensureLocalImageDir(context, comicId, downloadComic)
            val files = imageDir
                ?.let(::listComicImageFiles)
                .orEmpty()

            if (files.isEmpty()) {
                _comicPicState.update {
                    it.copy(
                        isLoading = false,
                        isError = true,
                        errorMsg = "未找到本地缓存图片"
                    )
                }
                return@launch
            }

            _comicPicState.update {
                it.copy(
                    data = files.mapIndexed { index, file ->
                        ComicPicImageState(
                            index = index,
                            comicId = comicId,
                            originSrc = file.absolutePath,
                            __scrambleId = Int.MAX_VALUE,
                            __speed = "1",
                            picImageLoader = picImageLoader
                        )
                    },
                    isLoading = false
                )
            }
            onSuccess?.invoke()
        }
    }

    private suspend fun loadLocalChapterList(comicId: Int, currentComic: DownloadComic?) {
        val groupId = currentComic?.groupId?.takeIf { it != 0 } ?: comicId
        val chapters = downloadComicDao.getCompleteByGroupId(groupId)
        _localChapterList.value = chapters.mapIndexed { index, item ->
            ComicChapter(
                id = item.id,
                name = item.chapterName.ifBlank {
                    if (chapters.size > 1) "第 ${index + 1} 章" else item.name
                }
            )
        }
    }

    private fun ensureLocalImageDir(context: Context, comicId: Int, downloadComic: DownloadComic?): File? {
        val zipPath = downloadComic?.zipPath.orEmpty()
        val directDir = zipPath.takeIf { it.isNotBlank() }?.let(::File)
        if (directDir?.isDirectory == true && listComicImageFiles(directDir).isNotEmpty()) {
            return directDir
        }

        if (downloadComic != null) {
            val namedDir = getComicChapterDownloadDir(context, downloadComic)
            if (namedDir.exists() && listComicImageFiles(namedDir).isNotEmpty()) {
                return namedDir
            }
            // 回退到旧版「纯章节名」目录，保证升级前下载的内容仍能打开
            val legacyDir = getLegacyComicChapterDownloadDir(context, downloadComic)
            if (legacyDir.exists() && listComicImageFiles(legacyDir).isNotEmpty()) {
                return legacyDir
            }
        }

        val dir = File(getDownloadDir(context), "$comicId")
        if (dir.exists() && dir.listFiles()?.isNotEmpty() == true) {
            return dir
        }
        if (zipPath.isBlank()) {
            return dir.takeIf { it.exists() }
        }
        val zipFile = File(zipPath)
        if (!zipFile.exists()) {
            return dir.takeIf { it.exists() }
        }
        dir.mkdirs()
        ZipInputStream(zipFile.inputStream()).use { zipIn ->
            while (true) {
                val entry = zipIn.nextEntry ?: break
                if (!entry.isDirectory) {
                    val output = File(dir, File(entry.name).name)
                    FileOutputStream(output).use { out ->
                        zipIn.copyTo(out)
                    }
                }
                zipIn.closeEntry()
            }
        }
        return dir
    }

    fun decodeIndex(index: Int, context: Context) {
        if (size <= 0 || index !in 0 until size) return
        val count = prefetchCount()
        val start = max(0, index - count)
        val end = min(size - 1, index + count)
        retainOnly(start, end)
        decode(index, context) {
            for (i in index + 1..end) decode(i, context)
            for (i in index - 1 downTo start) decode(i, context)
        }
    }

    fun decodeVisibleRange(firstIndex: Int, lastIndex: Int, context: Context) {
        if (size <= 0) return
        val count = prefetchCount()
        val start = max(0, min(firstIndex, lastIndex) - count)
        val end = min(size - 1, max(firstIndex, lastIndex) + count)
        retainOnly(start, end)
        for (i in start..end) {
            decode(i, context)
        }
    }

    /**
     * 只保留 [start]..[end]（两端各外扩 [RETAIN_MARGIN] 页）范围内已解码的页，其余释放。
     *
     * 记录「解码过」的集合必须能逐出：ImageResultState.Success 持有全尺寸位图，
     * 只增不减会让一次阅读会话翻过的每一页都常驻内存。
     */
    private fun retainOnly(start: Int, end: Int) {
        val keepStart = start - RETAIN_MARGIN
        val keepEnd = end + RETAIN_MARGIN
        val data = _comicPicState.value.data ?: return
        val iterator = decodedIndices.iterator()
        while (iterator.hasNext()) {
            val i = iterator.next()
            if (i in keepStart..keepEnd) continue
            decodeJobs.remove(i)?.cancel()
            data.getOrNull(i)?.release()
            iterator.remove()
        }
    }

    /** 释放全部已解码页，用于切换章节与 ViewModel 销毁 */
    private fun releaseAll() {
        // 必须先取快照并清空 map 再逐个 cancel：cancel() 可能同步触发
        // invokeOnCompletion 回调去 remove 同一个 map，边遍历边改会抛 CME
        val jobs = decodeJobs.values.toList()
        decodeJobs.clear()
        jobs.forEach { it.cancel() }
        _comicPicState.value.data?.forEach { it.release() }
        decodedIndices.clear()
    }

    fun prev(context: Context) {
        if (size <= 0) return
        hideToolBar()
        val index = max(0, currentIndexState.intValue - 1)
        currentIndexState.intValue = index
        decodeIndex(index, context)
    }

    fun next(context: Context) {
        if (size <= 0) return
        hideToolBar()
        val index = min(size - 1, currentIndexState.intValue + 1)
        currentIndexState.intValue = index
        decodeIndex(index, context)
    }

    private fun decode(index: Int, context: Context, onComplete: (() -> Unit)? = null) {
        val comicPicImageState = comicPicState.value.data?.getOrNull(index) ?: return
        // add 返回 false 说明这一页已经解码过或正在解码中
        if (!decodedIndices.add(index)) {
            onComplete?.invoke()
            return
        }
        val semaphore = getDecodeSemaphore()
        val job = viewModelScope.launch {
            try {
                semaphore.withPermit {
                    comicPicImageState.decode(context)
                }
            } catch (e: Exception) {
                log("decode index $index failed: ${e.message}")
            }
            onComplete?.invoke()
        }
        decodeJobs[index] = job
        job.invokeOnCompletion { decodeJobs.remove(index) }
    }

    override fun onCleared() {
        super.onCleared()
        releaseAll()
    }

    fun triggerToolBar() {
        isShowToolBar.value = !isShowToolBar.value
    }

    fun hideToolBar() {
        isShowToolBar.value = false
    }

    fun showToolBar() {
        isShowToolBar.value = true
    }
}
