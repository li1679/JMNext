package com.par9uet.jm.ui.feature.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.par9uet.jm.core.designsystem.component.TabSkeleton
import com.par9uet.jm.data.storage.LocalSettingManager
import com.par9uet.jm.domain.store.UserManager
import com.par9uet.jm.navigation.LocalMainNavController
import com.par9uet.jm.ui.component.Comic
import com.par9uet.jm.ui.component.ComicSkeleton
import com.par9uet.jm.ui.component.adaptiveComicGridCells
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.compose.getKoin
import org.koin.compose.viewmodel.koinActivityViewModel

private const val TEXT_DISCOVER = "\u53d1\u73b0\u6f2b\u753b"
private const val TEXT_FEATURED = "\u7cbe\u9009\u63a8\u8350"
private const val TEXT_SEARCH_HINT = "\u641c\u7d22\u4f5c\u54c1\u3001\u4f5c\u8005\u6216 tag"
private const val TEXT_WEEKLY = "\u6bcf\u5468"
private const val TEXT_DOWNLOAD = "\u4e0b\u8f7d"
private const val TEXT_SIGN = "\u7b7e\u5230"
private const val TEXT_EXTRACT = "\u63d0\u53d6"

@Composable
fun HomeScreen(
    comicViewModel: ComicViewModel = koinActivityViewModel(),
    userManager: UserManager = getKoin().get(),
    localSettingManager: LocalSettingManager = getKoin().get()
) {
    val mainNavController = LocalMainNavController.current
    val homeComicState by comicViewModel.homeComicState.collectAsStateWithLifecycle()
    val isLogin by userManager.isLoginState.collectAsStateWithLifecycle()
    val localSetting by localSettingManager.localSettingState.collectAsStateWithLifecycle()
    val onSearch = {
        // 搜索 ViewModel 是 Activity 级别的，首页入口代表新的搜索会话。
        comicViewModel.clearSearchState()
        mainNavController.navigate("comicSearch")
    }
    val onDownload = { mainNavController.navigate("download") }
    val onRecommend = { mainNavController.navigate("comicRecommend") }
    val onExtract = { mainNavController.navigate("extractCode") }
    val onSign = {
        if (isLogin) {
            mainNavController.navigate("sign")
        } else {
            mainNavController.navigate("login")
        }
    }

    // 只在首次进入或数据源变更时请求；从其它页面返回不重新拉取
    LaunchedEffect(Unit) {
        comicViewModel.ensureHomeComic()
    }

    val categories = homeComicState.list
    var selectedCategoryId by rememberSaveable { mutableStateOf<String?>(null) }
    var headerOffset by rememberSaveable { mutableFloatStateOf(0f) }
    OnTabScrollToTop("home") { headerOffset = 0f }
    var headerHeight by remember { mutableIntStateOf(0) }
    var viewportHeight by remember { mutableIntStateOf(0) }
    var tabsHeight by remember { mutableIntStateOf(0) }
    val minimumContentHeight = with(LocalDensity.current) { 96.dp.roundToPx() }
    val chipsScrollState = rememberScrollState()
    val gridStates = rememberSaveableStateHolder()
    val nestedScrollConnection = remember(minimumContentHeight) {
        object : NestedScrollConnection {
            private fun consume(delta: Float): Offset {
                val expandedHeight = minOf(
                    headerHeight,
                    (viewportHeight - tabsHeight - minimumContentHeight).coerceAtLeast(0),
                ).toFloat()
                val previous = headerOffset.coerceIn(-expandedHeight, 0f)
                headerOffset = (previous + delta).coerceIn(-expandedHeight, 0f)
                return Offset(0f, headerOffset - previous)
            }

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
                if (available.y < 0f) consume(available.y) else Offset.Zero

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset = if (available.y > 0f) consume(available.y) else Offset.Zero
        }
    }

    // Recreate only the pager when category order changes; restore by category ID.
    key(categories.map { it.id }) {
        val pagerState = rememberPagerState(
            initialPage = categories.indexOfFirst { it.id == selectedCategoryId }.coerceAtLeast(0),
            pageCount = { categories.size },
        )
        val scope = rememberCoroutineScope()
        val contentAlpha = remember { Animatable(1f) }
        var selectionJob by remember { mutableStateOf<Job?>(null) }
        LaunchedEffect(pagerState.settledPage) {
            categories.getOrNull(pagerState.settledPage)?.let { selectedCategoryId = it.id }
        }

        HomeLayout(
            modifier = Modifier.fillMaxSize().onSizeChanged { viewportHeight = it.height },
            headerOffset = { headerOffset },
            header = {
                Column(
                    Modifier.onSizeChanged { headerHeight = it.height }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    HomeHeader("", onSearch, onDownload, onRecommend, onExtract, onSign)
                }
            },
            tabs = {
                Column(
                    Modifier.fillMaxWidth().onSizeChanged { tabsHeight = it.height }
                        .padding(horizontal = 16.dp),
                ) {
                    if (categories.isEmpty() && homeComicState.isLoading) {
                        Row(
                            Modifier.fillMaxWidth().height(48.dp).horizontalScroll(chipsScrollState),
                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            repeat(6) { TabSkeleton(it) }
                        }
                    } else {
                        HomeCategoryChips(
                            categories = categories.map { it.title },
                            selectedIndex = pagerState.currentPage,
                            onSelect = { index ->
                                if (index != pagerState.currentPage || pagerState.isScrollInProgress) {
                                    selectionJob?.cancel()
                                    selectionJob = scope.launch {
                                        try {
                                            pagerState.scrollToPage(index)
                                            contentAlpha.snapTo(0.65f)
                                            contentAlpha.animateTo(1f, tween(150))
                                        } finally {
                                            contentAlpha.snapTo(1f)
                                        }
                                    }
                                }
                            },
                            scrollState = chipsScrollState,
                        )
                    }
                    HorizontalDivider()
                }
            },
        ) {
            PullToRefreshBox(
                modifier = Modifier.fillMaxSize(),
                isRefreshing = homeComicState.isLoading && categories.isNotEmpty(),
                onRefresh = comicViewModel::refreshHomeComic,
            ) {
                Box(Modifier.fillMaxSize().nestedScroll(nestedScrollConnection)) {
                    if (categories.isEmpty()) {
                        HomeComicGrid(
                            columns = adaptiveComicGridCells(localSetting.homeGridColumns),
                            comicList = emptyList(),
                            isLoading = homeComicState.isLoading,
                            error = if (homeComicState.isError) homeComicState.errorMsg ?: "加载失败" else null,
                            onRetry = comicViewModel::refreshHomeComic,
                        )
                    } else {
                        Column(Modifier.fillMaxSize()) {
                            if (homeComicState.isError) {
                                Text(
                                    text = homeComicState.errorMsg ?: "刷新失败",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                            HorizontalPager(
                                state = pagerState,
                                key = { categories[it].id },
                                modifier = Modifier.weight(1f).graphicsLayer {
                                    alpha = contentAlpha.value
                                },
                            ) { page ->
                                val category = categories[page]
                                gridStates.SaveableStateProvider(category.id) {
                                    Column {
                                        category.errorMessage?.let { message ->
                                            Text(message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
                                            Button(onClick = comicViewModel::refreshHomeComic) { Text("重试") }
                                        }
                                        HomeComicGrid(
                                            scrollToTopEnabled = page == pagerState.currentPage,
                                            columns = adaptiveComicGridCells(localSetting.homeGridColumns),
                                            comicList = category.list,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeLayout(
    modifier: Modifier,
    headerOffset: () -> Float,
    header: @Composable () -> Unit,
    tabs: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    Layout(
        modifier = modifier.clipToBounds(),
        content = {
            Box { header() }
            Box { tabs() }
            Box { content() }
        },
    ) { measurables, constraints ->
        val loose = constraints.copy(minHeight = 0)
        val headerPlaceable = measurables[0].measure(loose)
        val tabsPlaceable = measurables[1].measure(loose)
        // Short windows start partly collapsed so the list remains reachable.
        val expandedHeight = minOf(
            headerPlaceable.height,
            (constraints.maxHeight - tabsPlaceable.height - 96.dp.roundToPx()).coerceAtLeast(0),
        )
        val offset = headerOffset().roundToInt().coerceIn(-expandedHeight, 0)
        val contentTop = expandedHeight + offset + tabsPlaceable.height
        val contentHeight = (constraints.maxHeight - contentTop).coerceAtLeast(0)
        val contentPlaceable = measurables[2].measure(
            constraints.copy(minHeight = contentHeight, maxHeight = contentHeight),
        )
        layout(constraints.maxWidth, constraints.maxHeight) {
            headerPlaceable.placeRelative(0, expandedHeight - headerPlaceable.height + offset)
            tabsPlaceable.placeRelative(0, expandedHeight + offset)
            contentPlaceable.placeRelative(0, contentTop)
        }
    }
}

@Composable
private fun HomeComicGrid(
    columns: GridCells,
    comicList: List<com.par9uet.jm.core.model.Comic>,
    isLoading: Boolean = false,
    error: String? = null,
    onRetry: () -> Unit = {},
    scrollToTopEnabled: Boolean = true,
) {
    val gridState = rememberLazyGridState()
    OnTabScrollToTop("home", scrollToTopEnabled) { gridState.animateScrollToItem(0) }
    LazyVerticalGrid(
        modifier = Modifier.fillMaxSize(),
        columns = columns,
        state = gridState,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Top),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 20.dp)
    ) {
        if (isLoading) {
            items(18, key = { "skeleton-$it" }) {
                ComicSkeleton()
            }
        }
        if (error != null) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                    Button(onClick = onRetry) { Text("重试") }
                }
            }
        }
        items(items = comicList, key = { it.id }) {
            Comic(it)
        }
        if (comicList.isEmpty() && !isLoading && error == null) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Text(
                        text = "暂无漫画",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(
    categoryTitle: String,
    onSearch: () -> Unit,
    onDownload: () -> Unit,
    onRecommend: () -> Unit,
    onExtract: () -> Unit,
    onSign: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // 标题直接坐在页面底色上，不套彩色容器：
        // 首屏最显眼的位置留给内容，层级由字号、字重和留白建立。
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = TEXT_DISCOVER,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = categoryTitle.ifBlank { TEXT_FEATURED },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(MaterialTheme.shapes.medium)
                .clickable(onClick = onSearch),
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    modifier = Modifier.weight(1f),
                    text = TEXT_SEARCH_HINT,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HomeQuickAction(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Star,
                label = TEXT_WEEKLY,
                onClick = onRecommend
            )
            HomeQuickAction(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Password,
                label = TEXT_EXTRACT,
                onClick = onExtract
            )
            HomeQuickAction(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Download,
                label = TEXT_DOWNLOAD,
                onClick = onDownload
            )
            HomeQuickAction(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.CalendarMonth,
                label = TEXT_SIGN,
                onClick = onSign
            )
        }
    }
}

/** 首页分类按钮区 */
@Composable
private fun HomeCategoryChips(
    categories: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    scrollState: ScrollState
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEachIndexed { index, title ->
            key(title) {
                val bringIntoViewRequester = remember { BringIntoViewRequester() }
                LaunchedEffect(selectedIndex) {
                    if (selectedIndex == index) bringIntoViewRequester.bringIntoView()
                }
                FilterChip(
                    modifier = Modifier.bringIntoViewRequester(bringIntoViewRequester),
                    selected = selectedIndex == index,
                    onClick = { onSelect(index) },
                    label = {
                        Text(
                            text = title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelLarge
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                )
            }
        }
    }
}

@Composable
private fun HomeQuickAction(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(46.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
