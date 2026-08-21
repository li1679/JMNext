package com.par9uet.jm.domain.store

import com.par9uet.jm.core.common.ToastManager

import android.content.Context
import com.par9uet.jm.domain.cache.getCommonCacheDir
import com.par9uet.jm.core.common.formatBytes
import com.par9uet.jm.domain.notification.APP_UPDATE_NOTIFICATION_ID
import com.par9uet.jm.domain.notification.cancelProgressNotification
import com.par9uet.jm.domain.notification.showProgressNotification
import com.par9uet.jm.domain.notification.showUpdateDownloadedNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.roundToInt

enum class AppUpdateDownloadStatus {
    Idle,
    Downloading,
    Paused,
    Completed,
    Canceled,
    Error
}

data class AppUpdateDownloadState(
    val status: AppUpdateDownloadStatus = AppUpdateDownloadStatus.Idle,
    val version: String = "",
    val fileName: String = "",
    val downloadUrl: String = "",
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val speedBytesPerSecond: Long = 0L,
    val background: Boolean = false,
    val errorMessage: String = "",
    val savedPath: String = ""
) {
    val progress: Float
        get() = if (totalBytes > 0L) downloadedBytes.toFloat() / totalBytes else 0f
}

class AppUpdateDownloadManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val toastManager: ToastManager
) {
    private val client = OkHttpClient()
    private var job: Job? = null
    private var paused = false

    /**
     * 下载代次。
     *
     * start 与 cancel 都会让它自增，正在跑的旧任务据此判断自己是否已被取代。
     * 没有它时，start 先把 canceled 置 true 又立刻重置为 false，
     * 旧任务随后进入失败回调，会把刚开始的新任务状态改写成 Error。
     */
    private val generation = AtomicInteger(0)

    /** 正在飞行的请求。阻塞在 socket 读取时只有 cancel() 能把它叫醒 */
    @Volatile
    private var activeCall: Call? = null

    private var activeRequest: AppUpdateDownloadRequest? = null

    private val _state = MutableStateFlow(AppUpdateDownloadState())
    val state = _state.asStateFlow()

    fun start(request: AppUpdateDownloadRequest) {
        if (request.downloadUrl.isBlank()) {
            toastManager.showAsync("未找到 APK 下载链接")
            return
        }
        cancelInternal(resetState = false)
        activeRequest = request
        paused = false
        val myGeneration = generation.incrementAndGet()
        _state.value = AppUpdateDownloadState(
            status = AppUpdateDownloadStatus.Downloading,
            version = request.version,
            fileName = request.fileName,
            downloadUrl = request.downloadUrl
        )
        job = scope.launch {
            download(request, myGeneration)
        }
    }

    fun pause() {
        paused = true
        _state.update {
            if (it.status == AppUpdateDownloadStatus.Downloading) {
                it.copy(status = AppUpdateDownloadStatus.Paused, speedBytesPerSecond = 0L)
            } else {
                it
            }
        }
    }

    fun resume() {
        paused = false
        _state.update {
            if (it.status == AppUpdateDownloadStatus.Paused) {
                it.copy(status = AppUpdateDownloadStatus.Downloading)
            } else {
                it
            }
        }
    }

    fun cancel() {
        cancelInternal(resetState = true)
        cancelProgressNotification(context, APP_UPDATE_NOTIFICATION_ID)
    }

    fun sendToBackground() {
        _state.update { it.copy(background = true) }
        notifyProgress()
    }

    private fun cancelInternal(resetState: Boolean) {
        // 自增代次让在跑的任务立即失效，它随后写状态时会被 isCurrent 挡掉
        generation.incrementAndGet()
        paused = false
        // job.cancel() 对阻塞在 input.read() 的线程不起作用，必须同时断开连接
        activeCall?.cancel()
        activeCall = null
        job?.cancel()
        job = null
        if (resetState) {
            _state.update { it.copy(status = AppUpdateDownloadStatus.Canceled, speedBytesPerSecond = 0L) }
        }
    }

    private suspend fun download(
        request: AppUpdateDownloadRequest,
        myGeneration: Int
    ) = withContext(Dispatchers.IO) {
        fun isCurrent() = generation.get() == myGeneration
        runCatching {
            val httpRequest = Request.Builder()
                .url(request.downloadUrl)
                .header("User-Agent", "jmcomic-next-android")
                .build()
            val call = client.newCall(httpRequest).also { activeCall = it }
            call.execute().use { response ->
                if (!response.isSuccessful) {
                    error("下载失败：HTTP ${response.code}")
                }
                val body = response.body ?: error("下载失败：响应体为空")
                val totalBytes = body.contentLength().takeIf { it > 0L } ?: 0L
                val file = File(getCommonCacheDir(context), "updates/${request.fileName}")
                file.parentFile?.mkdirs()
                var downloaded = 0L
                var windowBytes = 0L
                var lastTick = System.currentTimeMillis()
                body.byteStream().use { input ->
                    FileOutputStream(file).use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            while (paused && isCurrent()) {
                                delay(250)
                            }
                            if (!isCurrent()) {
                                file.delete()
                                return@withContext
                            }
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            downloaded += read
                            windowBytes += read
                            val now = System.currentTimeMillis()
                            if (now - lastTick >= 500L) {
                                val speed = (windowBytes * 1000f / (now - lastTick)).roundToInt().toLong()
                                if (isCurrent()) {
                                    _state.update {
                                        it.copy(
                                            downloadedBytes = downloaded,
                                            totalBytes = totalBytes,
                                            speedBytesPerSecond = speed,
                                            status = AppUpdateDownloadStatus.Downloading
                                        )
                                    }
                                    notifyProgress()
                                }
                                windowBytes = 0L
                                lastTick = now
                            }
                        }
                    }
                }
                _state.update {
                    it.copy(
                        status = AppUpdateDownloadStatus.Completed,
                        downloadedBytes = downloaded,
                        totalBytes = totalBytes,
                        speedBytesPerSecond = 0L,
                        savedPath = file.absolutePath
                    )
                }
                cancelProgressNotification(context, APP_UPDATE_NOTIFICATION_ID)
                showUpdateDownloadedNotification(
                    context = context,
                    version = request.version,
                    savedPath = file.absolutePath
                )
            }
        }.onFailure { throwable ->
            // 已被取代或取消的任务不得改写状态，否则旧任务的失败会把
            // 刚启动的新任务显示成「下载失败」
            if (isCurrent()) {
                _state.update {
                    it.copy(
                        status = AppUpdateDownloadStatus.Error,
                        speedBytesPerSecond = 0L,
                        errorMessage = throwable.message ?: "下载失败"
                    )
                }
                cancelProgressNotification(context, APP_UPDATE_NOTIFICATION_ID)
            }
        }
        if (isCurrent()) activeCall = null
    }

    private fun notifyProgress() {
        val state = _state.value
        if (!state.background) return
        showProgressNotification(
            context = context,
            notificationId = APP_UPDATE_NOTIFICATION_ID,
            title = "正在下载更新 ${state.version}",
            text = "${(state.progress * 100).roundToInt()}% · ${formatBytes(state.speedBytesPerSecond)}/s",
            progressPercent = (state.progress * 100).roundToInt()
        )
    }
}

data class AppUpdateDownloadRequest(
    val version: String,
    val fileName: String,
    val downloadUrl: String
)
