package com.par9uet.jm.ui.feature.category

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.par9uet.jm.core.model.CategoryOrder
import com.par9uet.jm.data.storage.LocalSettingManager
import com.par9uet.jm.ui.component.Comic
import com.par9uet.jm.ui.component.PullRefreshAndLoadMoreGrid
import com.par9uet.jm.ui.component.adaptiveComicGridCells
import org.koin.compose.getKoin
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CategoryScreen(
    viewModel: CategoryViewModel = koinViewModel(),
    settings: LocalSettingManager = getKoin().get(),
) {
    val directory by viewModel.directory.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val localSetting by settings.localSettingState.collectAsStateWithLifecycle()
    val comics = viewModel.comics.collectAsLazyPagingItems()

    Column(Modifier.fillMaxSize()) {
        when {
            directory.loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
            directory.error != null -> CategoryMessage(directory.error!!, viewModel::loadDirectory)
            directory.categories.isEmpty() -> CategoryMessage("暂无分类", viewModel::loadDirectory)
            else -> {
                CategoryChoices(
                    choices = (if (directory.categories.none { it.slug == "0" }) listOf("0" to "全部") else emptyList()) +
                        directory.categories.map { it.slug to it.name },
                    selected = filter.categorySlug,
                    onSelect = viewModel::selectCategory,
                )
                val children = directory.categories.firstOrNull { it.slug == filter.categorySlug }?.children.orEmpty()
                if (children.isNotEmpty()) {
                    key(filter.categorySlug) {
                        CategoryChoices(
                            choices = listOf("" to "全部") + children.map { it.slug to it.name },
                            selected = filter.subCategorySlug.orEmpty(),
                            onSelect = { viewModel.selectSubCategory(it.takeIf(String::isNotEmpty)) },
                        )
                    }
                }
            }
        }
        CategoryChoices(
            choices = CategoryOrder.entries.map { it.name to it.label },
            selected = filter.order.name,
            onSelect = { viewModel.selectOrder(CategoryOrder.valueOf(it)) },
        )
        HorizontalDivider()
        val refresh = comics.loadState.refresh
        Box(Modifier.weight(1f).fillMaxWidth()) {
            // A new filter has its own scroll state; returning to this tab retains the current one.
            key(filter) {
                PullRefreshAndLoadMoreGrid(
                    lazyPagingItems = comics,
                    key = { it.id },
                    columns = adaptiveComicGridCells(localSetting.searchGridColumns),
                    modifier = Modifier.fillMaxSize(),
                ) { Comic(it) }
            }
            if (comics.itemCount == 0 && refresh is LoadState.Loading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
            if (refresh is LoadState.Error) {
                CategoryMessage(
                    refresh.error.message ?: "加载失败",
                    comics::retry,
                    Modifier.align(if (comics.itemCount == 0) Alignment.Center else Alignment.TopCenter),
                )
            } else if (comics.itemCount == 0 && refresh is LoadState.NotLoading &&
                comics.loadState.append.endOfPaginationReached) {
                CategoryMessage("暂无漫画", comics::refresh, Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
private fun CategoryChoices(choices: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        choices.forEach { (value, label) ->
            FilterChip(selected = value == selected, onClick = { onSelect(value) }, label = { Text(label) })
        }
    }
}

@Composable
private fun CategoryMessage(message: String, retry: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, style = MaterialTheme.typography.bodyMedium)
            Button(onClick = retry) { Text("重试") }
        }
    }
}
