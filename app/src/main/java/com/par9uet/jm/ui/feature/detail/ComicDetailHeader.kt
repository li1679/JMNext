package com.par9uet.jm.ui.feature.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.par9uet.jm.core.model.Comic
import com.par9uet.jm.domain.store.RemoteSettingManager
import com.par9uet.jm.ui.component.ComicCoverImage
import org.koin.compose.getKoin

/**
 * 详情页头图：模糊封面铺底 + 小封面 + 标题/作者/统计并排。
 *
 * 封面不铺满宽度：3:4 全宽会占掉约 70% 首屏，把标题、标签、按钮全推到折叠线以下。
 */
@Composable
internal fun ComicDetailHeader(
    comic: Comic,
    onAuthorClick: (String) -> Unit,
    remoteSettingManager: RemoteSettingManager = getKoin().get(),
    imageLoader: ImageLoader = getKoin().get(),
) {
    val remoteSetting by remoteSettingManager.remoteSettingState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val surface = MaterialTheme.colorScheme.surface
    val coverUrl = "${remoteSetting.imgHost}/media/albums/${comic.id}_3x4.jpg"

    Box(modifier = Modifier.fillMaxWidth()) {
        AsyncImage(
            model = remember(coverUrl) {
                ImageRequest.Builder(context).data(coverUrl).size(32).build()
            },
            imageLoader = imageLoader,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .matchParentSize()
                .backdropBlur()
        )
        // 遮罩：顶部与底部压回 surface，中段留出一点透，
        // 这样上接顶栏、下接正文都没有硬边界，而每本书又带上了自己的色调
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to surface,
                        0.28f to surface.copy(alpha = 0.55f),
                        1f to surface
                    )
                )
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ComicCoverImage(
                comic = comic,
                modifier = Modifier.width(HEADER_COVER_WIDTH)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = comic.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    comic.authorList.forEach {
                        key(it) {
                            Text(
                                modifier = Modifier.clickable(onClick = { onAuthorClick(it) }),
                                text = it,
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                Text(
                    text = "JM${comic.id}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    ComicStat(Icons.Default.Favorite, "喜欢", comic.likeCount)
                    ComicStat(Icons.Default.RemoveRedEye, "浏览", comic.readCount)
                }
            }
        }
    }
}

/** 带行首标题的标签分组。没有分组标题时，三排不同颜色的 chip 用户根本不知道各代表什么。 */
@Composable
internal fun TagGroup(
    title: String,
    tags: List<String>,
    tag: @Composable (String) -> Unit,
) {
    if (tags.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            tags.forEach { key(it) { tag(it) } }
        }
    }
}

/** 简介。数据里一直有 description 字段，但详情页从来没显示过。 */
@Composable
internal fun ComicDescription(description: String) {
    if (description.isBlank()) return
    var expanded by remember(description) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "简介",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = if (expanded) Int.MAX_VALUE else DESCRIPTION_COLLAPSED_LINES,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clickable { expanded = !expanded }
        )
        if (!expanded) {
            Text(
                text = "展开",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { expanded = true }
            )
        }
    }
}
