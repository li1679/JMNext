package com.par9uet.jm.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey

@Composable
fun <T : Any> PullRefreshAndLoadMoreGrid(
    modifier: Modifier = Modifier,
    lazyPagingItems: LazyPagingItems<T>,
    key: ((item: T) -> Any)?,
    columns: GridCells,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(10.dp, Alignment.Top),
    horizontalArrangement: Arrangement.HorizontalOrVertical = Arrangement.spacedBy(10.dp),
    contentPadding: PaddingValues = PaddingValues(10.dp),
    itemVisible: (item: T) -> Boolean = { true },
    enablePullRefresh: Boolean = true,
    state: LazyGridState = rememberLazyGridState(),
    header: (@Composable () -> Unit)? = null,
    itemContent: @Composable ((item: T) -> Unit),
) {
    val isRefreshing = lazyPagingItems.loadState.refresh is LoadState.Loading
    val gridContent: @Composable () -> Unit = {
        LazyVerticalGrid(
            modifier = Modifier.fillMaxSize(),
            columns = columns,
            state = state,
            verticalArrangement = verticalArrangement,
            horizontalArrangement = horizontalArrangement,
            contentPadding = contentPadding
        ) {
            if (header != null) {
                item(key = "grid-header", span = { GridItemSpan(maxLineSpan) }) { header() }
                if (lazyPagingItems.itemCount == 0 && isRefreshing) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
                val refreshError = lazyPagingItems.loadState.refresh as? LoadState.Error
                if (refreshError != null) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(refreshError.error.message ?: "加载失败", color = MaterialTheme.colorScheme.error)
                            Button(onClick = { lazyPagingItems.retry() }) { Text("重试") }
                        }
                    }
                }
            }
            items(
                lazyPagingItems.itemCount,
                key = key?.let { lazyPagingItems.itemKey(it) },
            ) { index ->
                val item = lazyPagingItems[index]
                if (item != null && itemVisible(item)) {
                    itemContent(item)
                }
            }
            when (val appendState = lazyPagingItems.loadState.append) {
                is LoadState.Loading -> {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                }

                is LoadState.Error -> {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("\u52a0\u8f7d\u5931\u8d25", color = MaterialTheme.colorScheme.error)
                            Button(onClick = { lazyPagingItems.retry() }) {
                                Text("\u91cd\u8bd5")
                            }
                        }
                    }
                }

                is LoadState.NotLoading -> {
                    if (appendState.endOfPaginationReached) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "\u6ca1\u6709\u66f4\u591a\u4e86",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    if (enablePullRefresh) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                lazyPagingItems.refresh()
            },
            modifier = modifier
        ) {
            gridContent()
        }
    } else {
        Box(modifier = modifier) {
            gridContent()
        }
    }
}
