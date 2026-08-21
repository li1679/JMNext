package com.par9uet.jm.ui.feature.settings

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.CheckCircle
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.par9uet.jm.core.model.ComicChapter
import com.par9uet.jm.domain.store.ComicGroupBackup

/** 缓存目录恢复：选择要重新下载的漫画。 */
/**
 * 将备份中的章节信息转换回 ComicChapter，用于恢复下载任务。
 */
internal fun ChapterBackup_to_ComicChapter(chapter: com.par9uet.jm.domain.store.ChapterBackup): ComicChapter {
    return ComicChapter(
        id = chapter.id,
        name = chapter.name,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ComicCacheRestoreDialog(
    groups: List<ComicGroupBackup>,
    imgHost: String,
    onConfirm: (List<ComicGroupBackup>) -> Unit,
    onSkip: () -> Unit,
    onDismiss: () -> Unit,
) {
    // 默认全部勾选
    val selectedIds = remember { mutableStateOf(groups.map { it.id }.toSet()) }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.widthIn(max = 520.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "恢复缓存目录",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "共 ${groups.size} 部漫画。勾选需要重新缓存的漫画，未勾选的不会恢复。恢复时会按编号重新创建缓存任务。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 全选/取消全选
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "已选 ${selectedIds.value.size} / ${groups.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = {
                        val allIds = groups.map { it.id }.toSet()
                        selectedIds.value = if (selectedIds.value.size == allIds.size) emptySet() else allIds
                    }) {
                        Text(if (selectedIds.value.size == groups.size) "取消全选" else "全选")
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(groups, key = { it.id }) { group ->
                        val checked = group.id in selectedIds.value
                        ComicRestoreRow(
                            group = group,
                            checked = checked,
                            imgHost = imgHost,
                            onToggle = {
                                selectedIds.value = if (checked) {
                                    selectedIds.value - group.id
                                } else {
                                    selectedIds.value + group.id
                                }
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Spacer(modifier = Modifier.size(4.dp))
                    TextButton(onClick = onSkip) { Text("跳过缓存恢复") }
                    Spacer(modifier = Modifier.size(4.dp))
                    TextButton(onClick = {
                        val selected = groups.filter { it.id in selectedIds.value }
                        onConfirm(selected)
                    }) { Text("恢复", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
internal fun ComicRestoreRow(
    group: ComicGroupBackup,
    checked: Boolean,
    imgHost: String,
    onToggle: () -> Unit,
) {
    val coverUrl = if (imgHost.isNotBlank()) {
        "${imgHost}/media/albums/${group.id}_3x4.jpg"
    } else ""
    val containerColor = if (checked) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceContainer
    val contentColor = if (checked) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurface

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(16.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp, 70.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (coverUrl.isNotBlank()) {
                    AsyncImage(
                        model = coverUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Book,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.name.ifBlank { "未命名漫画" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "共 ${group.chapterCount} 章" +
                        if (group.authors.isNotEmpty()) " · ${group.authors.joinToString("、")}" else "",
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
