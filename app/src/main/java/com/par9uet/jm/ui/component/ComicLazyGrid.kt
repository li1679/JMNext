package com.par9uet.jm.ui.component

import com.par9uet.jm.core.designsystem.component.LoadMore

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.par9uet.jm.core.model.Comic
import com.par9uet.jm.data.storage.LocalSettingManager
import com.par9uet.jm.core.common.filterBlockedTags
import org.koin.compose.getKoin

@Composable
fun ComicLazyGrid(
    list: List<Comic>,
    isRefreshing: Boolean,
    isMoreLoading: Boolean,
    hasMore: Boolean,
    pullToRefreshState: PullToRefreshState = rememberPullToRefreshState(),
    gridState: LazyGridState = rememberLazyGridState(),
    onRefresh: () -> Unit = {},
    onLoadMore: () -> Unit = {},
    modifier: Modifier = Modifier,
    columns: GridCells = adaptiveComicGridCells(),
    verticalArrangement: Arrangement.HorizontalOrVertical = Arrangement.spacedBy(10.dp),
    horizontalArrangement: Arrangement.HorizontalOrVertical = Arrangement.spacedBy(10.dp),
    contentPadding: PaddingValues = PaddingValues(8.dp),
    stickyHeaderContent: @Composable (() -> Unit)? = null,
    localSettingManager: LocalSettingManager = getKoin().get(),
) {
    val localSetting by localSettingManager.localSettingState.collectAsStateWithLifecycle()
    val visibleList = remember(list, localSetting.blockedTagList) {
        list.filterBlockedTags(localSetting.blockedTagList)
    }

    // layoutInfo 必须只在 derivedStateOf 的 lambda 内部读取。
    // 若作为 remember 的 key 在组合期读取，滚动每帧都会重组整个网格，
    // 且 visibleItemsInfo 每帧是新对象，会让 derivedStateOf 反复重建而失效。
    // 同理 LaunchedEffect 的 key 要用值而非 State 对象，否则会重复触发 onLoadMore。
    val shouldLoadMore by remember(gridState) {
        derivedStateOf {
            val layoutInfo = gridState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()
                ?: return@derivedStateOf false
            lastVisible.index >= layoutInfo.totalItemsCount - 1
        }
    }

    LaunchedEffect(shouldLoadMore, isRefreshing, hasMore, list.size) {
        if (shouldLoadMore && !isRefreshing && hasMore) {
            onLoadMore()
        }
    }

    // 整页内容都被标签排除时列表无可见项，也就没有「滚到底」事件可触发下一页，
    // 界面会永久停在空白状态。key 里带 list.size：只有真拿到新数据才会再次尝试，
    // 服务端不再返回新内容时自然停止。
    LaunchedEffect(list.size, visibleList.isEmpty(), hasMore, isRefreshing) {
        if (visibleList.isEmpty() && list.isNotEmpty() && hasMore && !isRefreshing) {
            onLoadMore()
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        state = pullToRefreshState,
        onRefresh = onRefresh,
        modifier = modifier
    ) {
        LazyVerticalGrid(
            state = gridState,
            columns = columns,
            verticalArrangement = verticalArrangement,
            horizontalArrangement = horizontalArrangement,
            contentPadding = contentPadding,
        ) {
            if (stickyHeaderContent !== null) {
                stickyHeader {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        stickyHeaderContent()
                    }
                }
            }
            items(
                items = visibleList,
                key = { it.id },
            ) { item ->
                Comic(item)
            }
            // visibleList 为空时也要保留：它是「滚到底」判定依赖的最后一项
            item(span = { GridItemSpan(maxLineSpan) }) {
                LoadMore(
                    isLoading = isMoreLoading,
                    hasMore = hasMore
                )
            }
        }
    }
    if (isRefreshing && list.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                // 禁止点击，让点击穿透
                .pointerInput(Unit) { },
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}
