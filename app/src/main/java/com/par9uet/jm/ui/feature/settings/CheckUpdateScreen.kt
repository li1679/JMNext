package com.par9uet.jm.ui.feature.settings

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Minimize
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.par9uet.jm.domain.store.AppUpdateDownloadManager
import com.par9uet.jm.domain.store.AppUpdateDownloadRequest
import com.par9uet.jm.domain.store.AppUpdateDownloadStatus
import com.par9uet.jm.core.common.formatBytes
import com.par9uet.jm.ui.component.CommonScaffold
import com.par9uet.jm.core.designsystem.util.MarkdownText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.compose.getKoin
import java.io.File
import kotlin.math.roundToInt
import com.par9uet.jm.core.common.appVersionCode
import com.par9uet.jm.core.common.appVersionName
import com.par9uet.jm.core.common.loadAppIconBitmap

private const val GITHUB_RELEASE_API =
    "https://api.github.com/repos/li1679/JMNext/releases/latest"
private const val GITHUB_RELEASE_URL =
    "https://github.com/li1679/JMNext/releases"

internal data class GithubRelease(
    val version: String,
    val name: String,
    val url: String,
    val body: String,
    val downloadUrl: String,
    val fileName: String
)

internal sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    data class Success(val release: GithubRelease, val hasUpdate: Boolean) : UpdateState()
    data class Error(val message: String) : UpdateState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckUpdateScreen(
    updateDownloadManager: AppUpdateDownloadManager = getKoin().get()
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val appIcon = remember(context) { loadAppIconBitmap(context) }
    val appVersion = remember(context) { appVersionName(context) }
    val versionCode = remember(context) { appVersionCode(context) }
    val coroutineScope = rememberCoroutineScope()
    val downloadState by updateDownloadManager.state.collectAsStateWithLifecycle()
    var updateState by remember { mutableStateOf<UpdateState>(UpdateState.Idle) }
    var visibleRelease by remember { mutableStateOf<GithubRelease?>(null) }
    var showDownloadDialog by remember { mutableStateOf(false) }

    fun checkUpdate() {
        updateState = UpdateState.Checking
        coroutineScope.launch {
            val nextState = fetchLatestRelease().fold(
                onSuccess = {
                    UpdateState.Success(
                        release = it,
                        hasUpdate = compareVersion(it.version, appVersion) > 0
                    )
                },
                onFailure = {
                    UpdateState.Error(it.message ?: "检查更新失败")
                }
            )
            updateState = nextState
            if (nextState is UpdateState.Success && nextState.hasUpdate) {
                visibleRelease = nextState.release
            }
        }
    }

    LaunchedEffect(Unit) {
        checkUpdate()
    }

    // 检查下载是否已完成，用于展示安装按钮
    val apkReady = downloadState.status == AppUpdateDownloadStatus.Completed &&
        downloadState.savedPath.isNotEmpty() &&
        File(downloadState.savedPath).exists()

    CommonScaffold(title = "检查更新") {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                CurrentVersionCard(appIcon, appVersion, versionCode)
            }
            item {
                UpdateStatusCard(
                    updateState = updateState,
                    appVersion = appVersion,
                    onRetry = { checkUpdate() },
                    onViewRelease = { visibleRelease = (updateState as? UpdateState.Success)?.release }
                )
            }
            if (apkReady) {
                item {
                    InstallCard(
                        version = downloadState.version,
                        onInstall = { installApk(context, downloadState.savedPath) }
                    )
                }
            }
        }
    }

    visibleRelease?.let { release ->
        ReleaseDialog(
            release = release,
            onCopyDownloadUrl = {
                clipboardManager.setText(AnnotatedString(release.downloadUrl.ifBlank { release.url }))
            },
            onDismiss = { visibleRelease = null },
            onDownload = {
                updateDownloadManager.start(
                    AppUpdateDownloadRequest(
                        version = release.version,
                        fileName = release.fileName.ifBlank { "jm-mobile_v${release.version}_unknown.apk" },
                        downloadUrl = release.downloadUrl
                    )
                )
                visibleRelease = null
                showDownloadDialog = true
            }
        )
    }

    if (showDownloadDialog && !downloadState.background) {
        UpdateDownloadDialog(
            onDismiss = { showDownloadDialog = false },
            onPauseResume = {
                if (downloadState.status == AppUpdateDownloadStatus.Paused) {
                    updateDownloadManager.resume()
                } else {
                    updateDownloadManager.pause()
                }
            },
            onCancel = {
                updateDownloadManager.cancel()
                showDownloadDialog = false
            },
            onBackground = {
                updateDownloadManager.sendToBackground()
                showDownloadDialog = false
            },
            downloadState = downloadState
        )
    }
}

private fun installApk(context: Context, savedPath: String) {
    val file = File(savedPath)
    if (!file.exists()) {
        android.widget.Toast.makeText(
            context,
            "安装包文件不存在，请重新下载",
            android.widget.Toast.LENGTH_LONG
        ).show()
        return
    }
    val intent = Intent(Intent.ACTION_VIEW).apply {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        setDataAndType(uri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching {
        context.startActivity(intent)
    }.onFailure { error ->
        com.par9uet.jm.core.common.logError("AppUpdate", "打开系统安装器失败: ${error.stackTraceToString()}")
        android.widget.Toast.makeText(
            context,
            "打开安装器失败：${error.message ?: "未知错误"}",
            android.widget.Toast.LENGTH_LONG
        ).show()
    }
}

private suspend fun fetchLatestRelease(): Result<GithubRelease> = withContext(Dispatchers.IO) {
    runCatching {
        val request = Request.Builder()
            .url(GITHUB_RELEASE_API)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "jmcomic-next-android")
            .build()
        OkHttpClient().newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("GitHub 返回 ${response.code}")
            }
            val body = response.body?.string() ?: error("GitHub 返回空响应")
            val json = JsonParser.parseString(body).asJsonObject
            val tagName = json.stringOrEmpty("tag_name")
            val name = json.stringOrEmpty("name")
            val url = json.stringOrEmpty("html_url")
            val version = normalizeVersion(tagName.ifBlank { name })
            if (version.isBlank()) {
                error("未读取到 Release 版本号")
            }
            val asset = selectApkAsset(json.getAsJsonArray("assets"), version)
            GithubRelease(
                version = version,
                name = name,
                url = url.ifBlank { "$GITHUB_RELEASE_URL/tag/$tagName" },
                body = json.stringOrEmpty("body"),
                downloadUrl = asset?.downloadUrl.orEmpty(),
                fileName = asset?.name.orEmpty()
            )
        }
    }
}

internal data class ReleaseAsset(val name: String, val downloadUrl: String)

private fun selectApkAsset(assets: JsonArray?, version: String): ReleaseAsset? {
    if (assets == null) return null
    val apkAssets = assets.mapNotNull { item ->
        val obj = item.asJsonObject
        val name = obj.stringOrEmpty("name")
        val url = obj.stringOrEmpty("browser_download_url")
        if (name.endsWith(".apk", ignoreCase = true) && url.isNotBlank()) {
            ReleaseAsset(name, url)
        } else {
            null
        }
    }
    return apkAssets.firstOrNull {
        it.name.contains("jm-mobile_v$version", ignoreCase = true)
    } ?: apkAssets.firstOrNull()
}

private fun JsonObject.stringOrEmpty(key: String): String {
    return get(key)?.takeIf { !it.isJsonNull }?.asString.orEmpty()
}

private fun normalizeVersion(value: String): String {
    return value.trim()
        .removePrefix("v")
        .removePrefix("V")
        .substringBefore(" ")
        .substringBefore("-")
}

private fun compareVersion(left: String, right: String): Int {
    val leftParts = normalizeVersion(left).split(".").map { it.toIntOrNull() ?: 0 }
    val rightParts = normalizeVersion(right).split(".").map { it.toIntOrNull() ?: 0 }
    val count = maxOf(leftParts.size, rightParts.size)
    for (index in 0 until count) {
        val l = leftParts.getOrElse(index) { 0 }
        val r = rightParts.getOrElse(index) { 0 }
        if (l != r) return l.compareTo(r)
    }
    return 0
}
