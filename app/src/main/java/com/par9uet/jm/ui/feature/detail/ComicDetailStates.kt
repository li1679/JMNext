package com.par9uet.jm.ui.feature.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.par9uet.jm.core.model.Comic
import com.par9uet.jm.ui.component.ComicContentTag
import com.par9uet.jm.ui.component.ComicRoleTag
import com.par9uet.jm.ui.component.ComicWorkTag
import com.par9uet.jm.core.designsystem.util.shimmer

/** 详情页的骨架屏、错误页与正文分节。 */

@Composable
internal fun ComicDetailSkeleton() {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 骨架屏要占住真实布局的位置，否则加载完成时内容会整体跳动
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    modifier = Modifier
                        .width(HEADER_COVER_WIDTH)
                        .aspectRatio(3f / 4f)
                        .clip(MaterialTheme.shapes.medium)
                        .shimmer()
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .clip(MaterialTheme.shapes.extraSmall)
                            .shimmer()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .height(20.dp)
                            .clip(MaterialTheme.shapes.extraSmall)
                            .shimmer()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(36.dp)
                            .clip(MaterialTheme.shapes.extraSmall)
                            .shimmer()
                    )
                }
            }
            repeat(3) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(if (it == 2) 0.6f else 1f)
                        .height(32.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .shimmer()
                )
            }
        }
    }
}

@Composable
internal fun ComicDetailErrorPage(
    errorMessage: String?,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Error,
                contentDescription = "加载失败",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = errorMessage ?: "加载失败，请稍后重试",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onRetry) {
                Text("重试")
            }
            TextButton(onClick = onBack) {
                Text("返回")
            }
        }
    }
}

/** 详情页正文：头图之外的所有分节，手机与平板布局共用 */
@Composable
internal fun ComicDetailSections(
    comic: Comic,
    onTagSearch: (String) -> Unit,
    onComments: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ComicDescription(comic.description)
        TagGroup("标签", comic.tagList) { ComicContentTag(it) }
        TagGroup("角色", comic.roleList) { ComicRoleTag(it) }
        TagGroup("作品", comic.workList) { ComicWorkTag(it) }
        HorizontalDivider()
        Surface(
            onClick = onComments,
            shape = MaterialTheme.shapes.medium,
            color = Color.Transparent,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.ChatBubbleOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "评论",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (comic.commentCount > 0) formatCount(comic.commentCount) else "暂无",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
