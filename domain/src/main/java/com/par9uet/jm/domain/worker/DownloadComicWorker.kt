package com.par9uet.jm.domain.worker

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import com.par9uet.jm.domain.cache.DownloadFileLocks
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import coil.ImageLoader
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.par9uet.jm.domain.cache.getComicChapterDownloadDir
import com.par9uet.jm.domain.cache.getComicCoverDownloadFile
import com.par9uet.jm.domain.cache.writeComicCacheConfig
import com.par9uet.jm.domain.image.ComicPicImageState
import com.par9uet.jm.domain.image.ImageResultState
import com.par9uet.jm.data.database.dao.DownloadComicDao
import com.par9uet.jm.data.database.model.DownloadComic
import com.par9uet.jm.data.database.model.UpdateComicCover
import com.par9uet.jm.data.database.model.UpdateComicProgress
import com.par9uet.jm.data.database.model.UpdateComicStatus
import com.par9uet.jm.data.database.model.UpdateComicZipPath
import com.par9uet.jm.data.repository.ComicRepository
import com.par9uet.jm.data.network.model.ComicPicListResponse
import com.par9uet.jm.data.network.model.NetWorkResult
import com.par9uet.jm.domain.store.DownloadToastAggregator
import com.par9uet.jm.data.storage.LocalSettingManager
import com.par9uet.jm.domain.store.RemoteSettingManager
import com.par9uet.jm.domain.notification.COMIC_CACHE_NOTIFICATION_ID_BASE
import com.par9uet.jm.core.common.DownloadSpeedTracker
import com.par9uet.jm.core.common.WEBP_QUALITY_COVER
import com.par9uet.jm.core.common.WEBP_QUALITY_DOWNLOAD
import com.par9uet.jm.domain.notification.cancelProgressNotification
import com.par9uet.jm.core.common.compressWebpCompat
import com.par9uet.jm.domain.notification.showProgressNotification
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.Mutex
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicInteger

private const val DOWNLOAD_PAGE_TIMEOUT_MS = 180_000L
private const val DOWNLOAD_MAX_ATTEMPTS = 6

/** 单章内同时下载的页数。串行下载一本 200 页要等很久，但并发太高又会被 CDN 限流。 */
private const val PAGE_CONCURRENCY = 4

/** 进度写库与通知刷新的最小间隔。逐页刷新会被系统限流，反而看不到进度。 */
private const val PROGRESS_THROTTLE_MS = 500L

class DownloadComicWorker(
    private val appContext: Context,
    params: WorkerParameters,
    private val downloadComicDao: DownloadComicDao,
    private val remoteSettingManager: RemoteSettingManager,
    private val localSettingManager: LocalSettingManager,
    private val comicRepository: ComicRepository,
    private val downloadToastAggregator: DownloadToastAggregator,
    // 必须复用 DI 里配好的 ImageLoader：自行 new 的实例不带 coil/Config.kt 中的
    // UA 与 Referer，会出现「在线能看、下载 403」这类难以排查的现象。
    private val picImageLoader: ImageLoader,
) : CoroutineWorker(appContext, params) {

    private val progressLock = Mutex()
    private var lastReportedDone = 0

    @Volatile
    private var lastProgressAt = 0L

    override suspend fun doWork(): Result {
        val comicId = inputData.getInt("comicId", -1)
        return DownloadFileLocks.chapter(comicId).withLock {
            currentCoroutineContext().ensureActive()
            runDownload()
        }
    }

    private suspend fun runDownload(): Result {
        val comicId = inputData.getInt("comicId", -1)
        val batchId = inputData.getString("batchId").orEmpty()
        val batchTotal = inputData.getInt("batchTotal", 1)
        if (comicId == -1) {
            return Result.failure()
        }

        val coverOwnerId = downloadComicDao.getById(comicId)?.let {
            it.groupId.takeIf { g -> g != 0 } ?: comicId
        } ?: comicId

        return try {
            val downloadTask = downloadComicDao.getById(comicId) ?: return Result.failure()
            downloadComicDao.updateStatus(UpdateComicStatus(comicId, "downloading"))
            DownloadSpeedTracker.startTracking(comicId, coverOwnerId)
            showComicCacheNotification(
                downloadTask,
                resolveGroupProgress(downloadTask, downloadTask.progress)
            )

            val coverPath = downloadCover(downloadTask, coverOwnerId)
            downloadComicDao.updateCover(UpdateComicCover(comicId, coverPath))

            downloadPicList(downloadTask)
            showComicCacheNotification(downloadTask, updateChapterProgress(downloadTask, 1f))

            val chapterDirPath = getComicChapterDownloadDir(appContext, downloadTask).absolutePath
            downloadComicDao.updateZipPath(UpdateComicZipPath(comicId, chapterDirPath))
            downloadComicDao.updateStatus(UpdateComicStatus(comicId, "complete"))
            writeCacheConfig(comicId)
            cancelComicCacheNotificationIfIdle(downloadTask)
            downloadToastAggregator.report(batchId, batchTotal, comicId, success = true)
            Result.success()
        } catch (e: CancellationException) {
            // 用户暂停或删除时 WorkManager 会取消协程。这里必须原样抛出：
            // 当成普通异常处理会走进重试分支，任务被取消后又排回来继续下载；
            // 也不能把状态改成 error，暂停的任务应保持 paused 等待继续。
            throw e
        } catch (e: Exception) {
            Log.e("DownloadComicWorker", "Download failed for chapter $comicId (attempt ${runAttemptCount + 1})", e)
            if (runAttemptCount < DOWNLOAD_MAX_ATTEMPTS - 1) {
                downloadComicDao.updateStatus(UpdateComicStatus(comicId, "pending"))
                downloadComicDao.getById(comicId)?.let { cancelComicCacheNotificationIfIdle(it) }
                Result.retry()
            } else {
                downloadComicDao.updateStatus(UpdateComicStatus(comicId, "error"))
                downloadComicDao.getById(comicId)?.let {
                    cancelComicCacheNotificationIfIdle(it)
                }
                downloadToastAggregator.report(batchId, batchTotal, comicId, success = false)
                Result.failure()
            }
        } finally {
            DownloadSpeedTracker.stopTracking(comicId)
        }
    }

    private suspend fun downloadCover(downloadTask: DownloadComic, coverOwnerId: Int): String {
        return withContext(Dispatchers.IO) {
            DownloadFileLocks.group(coverOwnerId).withLock {
                val file = getComicCoverDownloadFile(appContext, downloadTask)
                if (file.isFile && file.length() > 0) return@withLock file.absolutePath
                val coverUrl =
                    "${remoteSettingManager.remoteSettingState.value.imgHost}/media/albums/${coverOwnerId}_3x4.jpg"
                val request = ImageRequest.Builder(appContext)
                    .data(coverUrl)
                    .allowHardware(false)
                    .build()

                when (val result = withDownloadTimeout(DOWNLOAD_PAGE_TIMEOUT_MS) { picImageLoader.execute(request) }) {
                    is ErrorResult -> throw IllegalStateException("封面下载失败", result.throwable)
                    is SuccessResult -> {
                        val bitmap = result.drawable.toBitmap()
                        val tempFile = File.createTempFile("cover-", ".tmp", file.parentFile)
                        try {
                            val written = FileOutputStream(tempFile).use { out ->
                                bitmap.compressWebpCompat(WEBP_QUALITY_COVER, out)
                            }
                            check(written) { "封面文件写入失败" }
                            currentCoroutineContext().ensureActive()
                            Files.move(tempFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
                        } finally {
                            tempFile.delete()
                        }
                        file.absolutePath
                    }
                }
            }
        }
    }

    private suspend fun downloadPicList(downloadTask: DownloadComic): List<String> {
        return withContext(Dispatchers.IO) {
            val comicId = downloadTask.id
            when (val data = comicRepository.getComicPicList(comicId)) {
                is NetWorkResult.Error -> throw IllegalStateException(data.message)
                is NetWorkResult.Success<ComicPicListResponse> -> {
                    if (data.data.list.isEmpty()) {
                        throw IllegalStateException("图片列表为空")
                    }

                    val dir = getComicChapterDownloadDir(appContext, downloadTask)
                    val total = data.data.list.size
                    val semaphore = Semaphore(PAGE_CONCURRENCY)
                    val completed = AtomicInteger(0)

                    coroutineScope {
                        data.data.list.mapIndexed { index, url ->
                            async {
                                semaphore.withPermit {
                                    val path = downloadPage(
                                        downloadTask = downloadTask,
                                        dir = dir,
                                        index = index,
                                        url = url,
                                        response = data.data,
                                        speedOwnerId = downloadTask.id
                                    )
                                    reportProgress(
                                        downloadTask = downloadTask,
                                        done = completed.incrementAndGet(),
                                        total = total
                                    )
                                    path
                                }
                            }
                        }.awaitAll()
                    }
                }
            }
        }
    }

    private suspend fun downloadPage(
        downloadTask: DownloadComic,
        dir: File,
        index: Int,
        url: String,
        response: ComicPicListResponse,
        speedOwnerId: Int,
    ): String {
        val file = File(dir, "$index.webp")
        if (file.isFile) {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            if (bounds.outWidth > 0 && bounds.outHeight > 0) return file.absolutePath
            check(file.delete()) { "无法删除损坏的第 ${index + 1} 页" }
        }

        val imageState = ComicPicImageState(
            index = index,
            comicId = downloadTask.id,
            originSrc = url,
            __scrambleId = response.__scrambleId,
            __speed = response.__speed,
            picImageLoader = picImageLoader,
            __aId = response.__aId
        )
        try {
            withDownloadTimeout(DOWNLOAD_PAGE_TIMEOUT_MS) {
                imageState.decode(appContext)
            }
        } catch (e: CancellationException) {
            imageState.release()
            throw e
        } catch (e: Exception) {
            imageState.release()
            throw IllegalStateException("第 ${index + 1} 页下载或解码失败", e)
        }

        return when (val result = imageState.imageResultState) {
            is ImageResultState.Success -> {
                val tempFile = File(dir, "$index.webp.tmp")
                try {
                    val written = FileOutputStream(tempFile).use { out ->
                        result.decodeImageBitmap.asAndroidBitmap()
                            .compressWebpCompat(WEBP_QUALITY_DOWNLOAD, out)
                    }
                    check(written && tempFile.renameTo(file)) { "第 ${index + 1} 页文件写入失败" }
                } finally {
                    tempFile.delete()
                    imageState.release()
                }
                DownloadSpeedTracker.addBytes(speedOwnerId, file.length())
                file.absolutePath
            }

            is ImageResultState.Failure -> {
                throw IllegalStateException("第 ${index + 1} 页下载失败：${result.reason}")
            }

            ImageResultState.Loading -> {
                throw IllegalStateException("第 ${index + 1} 页仍在加载中")
            }
        }
    }

    /**
     * 汇报进度：按时间节流，避免每页都写库 + 发通知。
     */
    private suspend fun reportProgress(
        downloadTask: DownloadComic,
        done: Int,
        total: Int,
    ) {
        val chapterProgress = (done.toFloat() / total).coerceIn(0f, 1f)
        val now = System.currentTimeMillis()
        val isLast = done >= total
        progressLock.withLock {
            if (done <= lastReportedDone || (!isLast && now - lastProgressAt < PROGRESS_THROTTLE_MS)) return
            lastProgressAt = now
            lastReportedDone = done
            DownloadFileLocks.group(downloadTask.groupId.takeIf { it != 0 } ?: downloadTask.id).withLock {
                downloadComicDao.updateProgress(UpdateComicProgress(downloadTask.id, chapterProgress))
                val groupProgress = resolveGroupProgress(downloadTask, chapterProgress)
                showComicCacheNotification(downloadTask, groupProgress)
            }
        }
    }

    private suspend fun writeCacheConfig(comicId: Int) {
        val current = downloadComicDao.getById(comicId) ?: return
        val groupId = current.groupId.takeIf { it != 0 } ?: current.id
        DownloadFileLocks.group(groupId).withLock {
            val chapters = downloadComicDao.getByGroupId(groupId)
            withContext(Dispatchers.IO) {
                writeComicCacheConfig(appContext, current, chapters)
            }
        }
    }

    private suspend fun updateChapterProgress(downloadTask: DownloadComic, progress: Float): Float {
        val chapterProgress = progress.coerceIn(0f, 1f)
        downloadComicDao.updateProgress(UpdateComicProgress(downloadTask.id, chapterProgress))
        return resolveGroupProgress(downloadTask, chapterProgress)
    }

    private suspend fun resolveGroupProgress(downloadTask: DownloadComic, currentProgress: Float): Float {
        val groupId = downloadTask.groupId.takeIf { it != 0 } ?: downloadTask.id
        val chapters = downloadComicDao.getByGroupId(groupId)
        if (chapters.isEmpty()) return currentProgress
        return chapters.map { chapter ->
            when {
                chapter.id == downloadTask.id -> currentProgress
                chapter.status == "complete" -> 1f
                else -> chapter.progress.coerceIn(0f, 1f)
            }
        }.average().toFloat().coerceIn(0f, 1f)
    }

    private suspend fun cancelComicCacheNotificationIfIdle(downloadTask: DownloadComic) {
        val groupId = downloadTask.groupId.takeIf { it != 0 } ?: downloadTask.id
        val chapters = downloadComicDao.getByGroupId(groupId)
        val hasActiveTask = chapters.any { it.status == "downloading" }
        if (!hasActiveTask) {
            cancelProgressNotification(appContext, COMIC_CACHE_NOTIFICATION_ID_BASE + groupId)
        }
    }

    private fun showComicCacheNotification(downloadTask: DownloadComic, progress: Float) {
        val groupId = downloadTask.groupId.takeIf { it != 0 } ?: downloadTask.id
        val setting = localSettingManager.localSettingState.value
        if (!setting.showComicCacheNotification) {
            cancelProgressNotification(appContext, COMIC_CACHE_NOTIFICATION_ID_BASE + groupId)
            return
        }
        val comicName = downloadTask.groupName.ifBlank { downloadTask.name }
        val title = if (setting.showComicCacheNotificationName && comicName.isNotBlank()) {
            "正在缓存$comicName"
        } else {
            "正在缓存漫画"
        }
        val progressPercent = (progress.coerceIn(0f, 1f) * 100).toInt()
        showProgressNotification(
            context = appContext,
            notificationId = COMIC_CACHE_NOTIFICATION_ID_BASE + groupId,
            title = title,
            text = "$progressPercent%",
            progressPercent = progressPercent
        )
    }

}
