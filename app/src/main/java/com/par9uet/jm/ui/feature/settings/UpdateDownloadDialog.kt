package com.par9uet.jm.ui.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Minimize
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.par9uet.jm.domain.store.AppUpdateDownloadStatus
import com.par9uet.jm.core.common.formatBytes
import kotlin.math.roundToInt


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun UpdateDownloadDialog(
    onDismiss: () -> Unit,
    onPauseResume: () -> Unit,
    onCancel: () -> Unit,
    onBackground: () -> Unit,
    downloadState: com.par9uet.jm.domain.store.AppUpdateDownloadState
) {
    val status = downloadState.status
    val isDone = status == AppUpdateDownloadStatus.Completed ||
        status == AppUpdateDownloadStatus.Error ||
        status == AppUpdateDownloadStatus.Canceled
    val isPaused = status == AppUpdateDownloadStatus.Paused

    BasicAlertDialog(
        onDismissRequest = { if (isDone) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = isDone)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "下载更新",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isDone) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Rounded.Close, contentDescription = "关闭")
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = downloadState.fileName.ifBlank { "更新包" },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val statusText = when (status) {
                        AppUpdateDownloadStatus.Downloading -> "下载中"
                        AppUpdateDownloadStatus.Paused -> "已暂停"
                        AppUpdateDownloadStatus.Completed -> "下载完成，正在安装..."
                        AppUpdateDownloadStatus.Canceled -> "已取消"
                        AppUpdateDownloadStatus.Error -> "下载失败：${downloadState.errorMessage}"
                        AppUpdateDownloadStatus.Idle -> "等待下载"
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = when (status) {
                            AppUpdateDownloadStatus.Completed -> MaterialTheme.colorScheme.primary
                            AppUpdateDownloadStatus.Error -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
                if (status == AppUpdateDownloadStatus.Downloading || isPaused) {
                    LinearProgressIndicator(
                        progress = { downloadState.progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                } else if (status == AppUpdateDownloadStatus.Completed) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                } else {
                    LinearProgressIndicator(
                        progress = { downloadState.progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${(downloadState.progress * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${formatBytes(downloadState.downloadedBytes)} / " +
                            if (downloadState.totalBytes > 0) formatBytes(downloadState.totalBytes) else "未知大小",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (status == AppUpdateDownloadStatus.Downloading) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${formatBytes(downloadState.speedBytesPerSecond)}/s",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onCancel,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(if (isDone) "关闭" else "取消")
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    if (status != AppUpdateDownloadStatus.Completed) {
                        TextButton(
                            onClick = onBackground,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Rounded.Minimize, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("后台下载")
                        }
                    }
                    if (status == AppUpdateDownloadStatus.Downloading || isPaused) {
                        FilledTonalButton(
                            onClick = onPauseResume,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(
                                if (isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isPaused) "继续" else "暂停")
                        }
                    }
                }
            }
        }
    }
}

