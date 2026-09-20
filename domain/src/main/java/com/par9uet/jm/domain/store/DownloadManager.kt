package com.par9uet.jm.domain.store

import com.par9uet.jm.core.common.ToastManager

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.par9uet.jm.core.model.Comic
import com.par9uet.jm.core.model.ComicChapter
import com.par9uet.jm.data.database.dao.DownloadComicDao
import com.par9uet.jm.data.database.model.DownloadComic
import com.par9uet.jm.domain.worker.DownloadComicWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.CancellationException
import com.par9uet.jm.domain.cache.DownloadFileLocks
import com.par9uet.jm.domain.cache.comicChapterDownloadCandidates
import com.par9uet.jm.domain.cache.getComicDownloadRootDir
import com.par9uet.jm.domain.cache.getDownloadDir
import com.par9uet.jm.domain.cache.writeComicCacheConfig
import com.par9uet.jm.domain.notification.COMIC_CACHE_NOTIFICATION_ID_BASE
import com.par9uet.jm.domain.notification.cancelProgressNotification
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

private const val DOWNLOAD_RETRY_BACKOFF_SECONDS = 30L

/** 每话一个唯一任务名，供入队与取消共同定位 */
private fun workName(comicId: Int) = "download-comic-$comicId"

class DownloadManager(
    private val context: Context,
    private val downloadComicDao: DownloadComicDao,
    private val scope: CoroutineScope,
    private val toastManager: ToastManager,
) {
    private val commands = Mutex()

    private fun launchCommand(block: suspend () -> Unit) {
        scope.launch(Dispatchers.IO) {
            commands.withLock {
                try {
                    block()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    toastManager.showAsync("下载操作失败：${e.message}")
                }
            }
        }
    }

    fun downloadComic(comic: Comic) {
        launchCommand command@ {
            if (downloadComicDao.getExistingIds(listOf(comic.id)).isNotEmpty()) {
                toastManager.showAsync("该漫画已在缓存列表中")
                return@command
            }
            insertComicTask(comic)
            toastManager.showAsync("创建缓存任务成功")
            enqueueDownload(comic.id)
        }
    }

    fun downloadComics(comics: List<Comic>) {
        if (comics.isEmpty()) return
        launchCommand command@ {
            val existingIds = downloadComicDao.getExistingIds(comics.map { it.id }).toSet()
            val newComics = comics.filterNot { it.id in existingIds }
            if (newComics.isEmpty()) {
                toastManager.showAsync("所选漫画已在缓存列表中")
                return@command
            }

            newComics.forEach { insertComicTask(it) }
            enqueueDownloads(newComics.map { it.id })

            val skippedCount = comics.size - newComics.size
            toastManager.showAsync(
                if (skippedCount > 0) {
                    "已创建 ${newComics.size} 个缓存任务，跳过 $skippedCount 个已存在漫画"
                } else {
                    "已创建 ${newComics.size} 个缓存任务"
                }
            )
        }
    }

    fun downloadChapters(parentComic: Comic, chapters: List<ComicChapter>) {
        if (chapters.isEmpty()) return
        launchCommand command@ {
            val existingIds = downloadComicDao.getExistingIds(chapters.map { it.id }).toSet()
            val newChapters = chapters.filterNot { it.id in existingIds }
            if (newChapters.isEmpty()) {
                toastManager.showAsync("所选章节已在缓存列表中")
                return@command
            }

            val now = System.currentTimeMillis()
            newChapters.forEachIndexed { index, chapter ->
                downloadComicDao.insert(
                    DownloadComic(
                        id = chapter.id,
                        name = "${parentComic.name} ${chapter.name}".trim(),
                        authorList = parentComic.authorList,
                        tagList = parentComic.tagList,
                        coverPath = "",
                        zipPath = "",
                        progress = 0f,
                        status = "pending",
                        createTime = now + index,
                        groupId = parentComic.id,
                        groupName = parentComic.name,
                        chapterName = chapter.name
                    )
                )
            }
            enqueueDownloads(newChapters.map { it.id })

            val skippedCount = chapters.size - newChapters.size
            toastManager.showAsync(
                if (skippedCount > 0) {
                    "已创建 ${newChapters.size} 个缓存任务，跳过 $skippedCount 个已存在章节"
                } else {
                    "已创建 ${newChapters.size} 个缓存任务"
                }
            )
        }
    }

    private suspend fun insertComicTask(comic: Comic) {
        downloadComicDao.insert(
            DownloadComic(
                id = comic.id,
                name = comic.name,
                authorList = comic.authorList,
                tagList = comic.tagList,
                coverPath = "",
                zipPath = "",
                progress = 0f,
                status = "pending",
                createTime = System.currentTimeMillis(),
                groupId = comic.id,
                groupName = comic.name
            )
        )
    }

    private fun enqueueDownload(comicId: Int) {
        enqueueDownloads(listOf(comicId))
    }

    private fun enqueueDownloads(comicIds: List<Int>) {
        if (comicIds.isEmpty()) return
        val distinctComicIds = comicIds.distinct()
        val batchId = if (distinctComicIds.size > 1) UUID.randomUUID().toString() else ""
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val workManager = WorkManager.getInstance(context)
        distinctComicIds.forEach { comicId ->
            val downloadRequest = OneTimeWorkRequestBuilder<DownloadComicWorker>()
                .setConstraints(constraints)
                .setInputData(
                    workDataOf(
                        "comicId" to comicId,
                        "batchId" to batchId,
                        "batchTotal" to distinctComicIds.size
                    )
                )
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    DOWNLOAD_RETRY_BACKOFF_SECONDS,
                    TimeUnit.SECONDS
                )
                .build()
            // Explicit restart paths await cancellation before enqueue; duplicate requests keep existing work.
            workManager.enqueueUniqueWork(
                workName(comicId),
                ExistingWorkPolicy.KEEP,
                downloadRequest
            )
        }
    }

    /**
     * 取消这些话正在进行的下载任务。
     *
     * 暂停与删除都必须先走这里：只改数据库状态或删记录并不会让 Worker 停下，
     * 它会继续下载、继续写文件，最后把状态改回 complete。
     */
    suspend fun cancelDownloads(comicIds: List<Int>) = withContext(Dispatchers.IO) {
        if (comicIds.isEmpty()) return@withContext
        val workManager = WorkManager.getInstance(context)
        comicIds.distinct().forEach { workManager.cancelUniqueWork(workName(it)).result.get() }
        // WorkManager cancellation completes before a blocking image/file write necessarily stops.
        comicIds.distinct().forEach { DownloadFileLocks.chapter(it).withLock { } }
        val groupIds = comicIds.mapNotNull { id ->
            downloadComicDao.getById(id)?.let { it.groupId.takeIf { group -> group != 0 } ?: it.id }
        }.distinct()
        groupIds.forEach { groupId ->
            if (downloadComicDao.getByGroupId(groupId).none { it.id !in comicIds && it.status == "downloading" }) {
                cancelProgressNotification(context, COMIC_CACHE_NOTIFICATION_ID_BASE + groupId)
            }
        }
    }

    suspend fun deleteDownloads(comicIds: List<Int>): Boolean = withContext(Dispatchers.IO) {
        commands.withLock {
            try {
                cancelDownloads(comicIds)
                comicIds.distinct().forEach { id ->
                    DownloadFileLocks.chapter(id).withLock chapterLock@ {
                        val item = downloadComicDao.getById(id) ?: return@chapterLock
                        val groupId = item.groupId.takeIf { it != 0 } ?: item.id
                        DownloadFileLocks.group(groupId).withLock {
                            deleteChapterFiles(item)
                            val remaining = downloadComicDao.getByGroupId(groupId).filter { it.id != id }
                            if (remaining.isEmpty()) {
                                val root = getComicDownloadRootDir(context, item)
                                check(root.deleteRecursively()) { "无法删除下载目录：${root.name}" }
                                item.coverPath.takeIf { it.isNotBlank() }?.let(::File)?.let { cover ->
                                    if (cover.exists() && downloadComicDao.getAll().none { it.id != id && it.coverPath == cover.absolutePath }) {
                                        check(cover.delete()) { "无法删除封面：${cover.name}" }
                                    }
                                }
                            } else {
                                writeComicCacheConfig(context, remaining.first(), remaining)
                            }
                            downloadComicDao.deleteByIds(listOf(id))
                        }
                    }
                }
                true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                toastManager.showAsync("删除下载失败：${e.message}")
                false
            }
        }
    }

    suspend fun pauseDownloads(comicIds: List<Int>) = withContext(Dispatchers.IO) {
        commands.withLock {
            cancelDownloads(comicIds)
            downloadComicDao.updateStatusByIds(comicIds, "paused")
        }
    }

    private fun deleteChapterFiles(item: DownloadComic) {
        val paths = comicChapterDownloadCandidates(context, item) + File(getDownloadDir(context), item.id.toString())
        paths.forEach { file ->
            if (file.exists()) check(file.deleteRecursively()) { "无法删除章节文件：${file.name}" }
        }
    }

    fun retryDownload(comicId: Int) {
        launchCommand command@ {
            downloadComicDao.getById(comicId) ?: return@command
            cancelDownloads(listOf(comicId))
            downloadComicDao.updateProgress(
                com.par9uet.jm.data.database.model.UpdateComicProgress(comicId, 0f)
            )
            downloadComicDao.updateStatus(
                com.par9uet.jm.data.database.model.UpdateComicStatus(comicId, "pending")
            )
            enqueueDownload(comicId)
            toastManager.showAsync("已重新加入下载队列")
        }
    }

    /**
     * 恢复已暂停的下载任务：更新状态为 pending 并重新入队 WorkManager。
     * 与 retryDownload 不同，不会重置已下载进度，而是从断点继续。
     */
    fun resumeDownloads(comicIds: List<Int>) {
        if (comicIds.isEmpty()) return
        launchCommand command@ {
            val validIds = comicIds.filter { id ->
                val task = downloadComicDao.getById(id)
                task != null && task.status != "complete"
            }.distinct()
            if (validIds.isEmpty()) {
                toastManager.showAsync("没有可恢复的下载任务")
                return@command
            }
            cancelDownloads(validIds)
            downloadComicDao.updateStatusByIds(validIds, "pending")
            enqueueDownloads(validIds)
            toastManager.showAsync("已恢复 ${validIds.size} 个下载任务")
        }
    }

    fun retryGroup(groupId: Int) {
        launchCommand command@ {
            val chapters = downloadComicDao.getByGroupId(groupId)
            val errorIds = chapters.filter { it.status == "error" }.map { it.id }
            if (errorIds.isEmpty()) return@command
            cancelDownloads(errorIds)
            downloadComicDao.updateStatusByIds(errorIds, "pending")
            errorIds.forEach { id ->
                downloadComicDao.updateProgress(
                    com.par9uet.jm.data.database.model.UpdateComicProgress(id, 0f)
                )
            }
            enqueueDownloads(errorIds)
            toastManager.showAsync("已重新加入 ${errorIds.size} 个下载任务")
        }
    }

    fun redownloadGroup(groupId: Int) {
        launchCommand command@ {
            val items = downloadComicDao.getByGroupId(groupId)
            if (items.isEmpty()) return@command
            cancelDownloads(items.map { it.id })
            items.forEach { item ->
                deleteChapterFiles(item)
                downloadComicDao.updateStatus(
                    com.par9uet.jm.data.database.model.UpdateComicStatus(item.id, "pending")
                )
                downloadComicDao.updateProgress(
                    com.par9uet.jm.data.database.model.UpdateComicProgress(item.id, 0f)
                )
            }
            enqueueDownloads(items.map { it.id })
            toastManager.showAsync("已重新下载 ${items.size} 个任务")
        }
    }
}
