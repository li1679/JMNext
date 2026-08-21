package com.par9uet.jm.ui.feature.settings

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Circle
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.par9uet.jm.domain.store.BackupContentOptions
import com.par9uet.jm.domain.store.BackupFile

/** 备份 / 恢复的内容项选择对话框。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BackupContentPickerDialog(
    options: BackupContentOptions,
    onChange: (BackupContentOptions) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.widthIn(max = 440.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "选择备份内容",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                ContentToggleRow(
                    icon = Icons.Rounded.Info,
                    title = "本地设置",
                    subtitle = "标签排除、配色方案、推荐方式、网格列数、阅读设置等",
                    checked = options.includeLocalSetting,
                    onCheckedChange = { onChange(options.copy(includeLocalSetting = it)) }
                )
                ContentToggleRow(
                    icon = Icons.Rounded.Book,
                    title = "缓存目录",
                    subtitle = "只备份漫画编号与章节信息，不备份图片文件",
                    checked = options.includeComicCache,
                    onCheckedChange = { onChange(options.copy(includeComicCache = it)) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Spacer(modifier = Modifier.size(8.dp))
                    TextButton(onClick = onConfirm) { Text("下一步", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RestoreContentPickerDialog(
    backup: BackupFile,
    onConfirm: (BackupContentOptions) -> Unit,
    onDismiss: () -> Unit,
) {
    var localSettingOn by remember { mutableStateOf(backup.meta.includeLocalSetting) }
    var comicCacheOn by remember { mutableStateOf(backup.meta.includeComicCache) }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.widthIn(max = 440.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "选择恢复内容",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (backup.meta.includeLocalSetting) {
                    ContentToggleRow(
                        icon = Icons.Rounded.Info,
                        title = "本地设置",
                        subtitle = "会覆盖当前本地设置",
                        checked = localSettingOn,
                        onCheckedChange = { localSettingOn = it }
                    )
                }
                if (backup.meta.includeComicCache) {
                    ContentToggleRow(
                        icon = Icons.Rounded.Book,
                        title = "缓存目录",
                        subtitle = "共 ${backup.meta.comicCacheCount} 部漫画，恢复时可选择具体内容",
                        checked = comicCacheOn,
                        onCheckedChange = { comicCacheOn = it }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Spacer(modifier = Modifier.size(8.dp))
                    TextButton(onClick = {
                        onConfirm(
                            BackupContentOptions(
                                includeLocalSetting = localSettingOn,
                                includeComicCache = comicCacheOn,
                            )
                        )
                    }) { Text("下一步", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
internal fun ContentToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val containerColor = if (checked) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceContainer
    val contentColor = if (checked) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurface
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        shape = RoundedCornerShape(16.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (checked) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = if (checked) Icons.Rounded.CheckCircle else Icons.Rounded.Circle,
                contentDescription = null,
                tint = if (checked) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

