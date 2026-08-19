package com.par9uet.jm.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.par9uet.jm.core.model.Comic
import com.par9uet.jm.navigation.LocalMainNavController
import com.par9uet.jm.ui.feature.detail.ComicDetailViewModel
import org.koin.compose.viewmodel.koinActivityViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Comic(
    comic: Comic,
    modifier: Modifier = Modifier,
    editing: Boolean = false,
    selected: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    onToggleSelected: (() -> Unit)? = null,
    /** 打开详情前清理调用方的编辑/选择状态，避免返回列表后残留旧操作栏。 */
    onBeforeOpenDetail: () -> Unit = {},
    comicDetailViewModel: ComicDetailViewModel = koinActivityViewModel()
) {
    val mainNavController = LocalMainNavController.current

    // 封面自身就是内容，不再套卡片容器、也不加投影：
    // 网格里一屏几十张封面，每张都带阴影会让整个页面发灰。
    // 分隔完全交给留白。
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .combinedClickable(
                onClick = {
                    if (editing && onToggleSelected != null) {
                        onToggleSelected()
                    } else {
                        onBeforeOpenDetail()
                        comicDetailViewModel.reset(comic.id)
                        mainNavController.navigate("comicDetail/${comic.id}") {
                            launchSingleTop = true
                        }
                    }
                },
                onLongClick = {
                    onLongClick?.invoke()
                }
            ),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box {
            ComicCoverImage(
                comic = comic,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (editing && selected) {
                            Modifier.border(
                                width = 3.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = MaterialTheme.shapes.medium
                            )
                        } else {
                            Modifier
                        }
                    )
            )
            if (editing && selected) {
                Checkbox(
                    checked = true,
                    onCheckedChange = null,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                )
            }
        }
        Text(
            text = comic.name,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            // minLines 与 maxLines 同为 2：漫画标题普遍很长且常带 [作者] 前缀，
            // 一行放不下；固定两行可保证同一网格行的文字块等高。
            maxLines = 2,
            minLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        // 无作者时整行不渲染，避免出现占位文案
        val author = comic.authorList.joinToString("、").takeIf { it.isNotBlank() }
        if (author != null) {
            Text(
                text = author,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
