package com.par9uet.jm.ui.feature.user

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DriveFileMove
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


// 编辑模式底部工具栏：Material 3 BottomAppBar
@Composable
internal fun CollectEditBottomAppBar(
    selectedCount: Int,
    onClose: () -> Unit,
    onCache: () -> Unit,
    onDelete: () -> Unit,
    onMove: () -> Unit
) {
    BottomAppBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        actions = {
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.Close, contentDescription = "退出编辑")
            }
            Text(
                text = "已选择 $selectedCount 项",
                modifier = Modifier.padding(start = 4.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onCache) {
                Icon(Icons.Rounded.Download, contentDescription = "缓存")
            }
            IconButton(onClick = onMove) {
                Icon(Icons.AutoMirrored.Rounded.DriveFileMove, contentDescription = "移动")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.Delete, contentDescription = "取消收藏")
            }
        }
    )
}
