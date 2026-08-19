package com.par9uet.jm.ui.feature.detail

import com.par9uet.jm.navigation.LocalMainNavController

import android.net.Uri
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.par9uet.jm.core.model.Comic
import com.par9uet.jm.data.storage.ComicReadHistory
import com.par9uet.jm.domain.store.DownloadManager
import com.par9uet.jm.domain.store.ReadHistoryManager
import com.par9uet.jm.domain.store.RemoteSettingManager
import com.par9uet.jm.domain.store.UserManager
import com.par9uet.jm.ui.component.ChapterMultiSelectDialog
import com.par9uet.jm.ui.component.ComicContentTag
import com.par9uet.jm.ui.component.ComicCoverImage
import com.par9uet.jm.ui.component.ComicRoleTag
import com.par9uet.jm.ui.component.ComicWorkTag
import com.par9uet.jm.ui.feature.detail.ComicDetailViewModel
import com.par9uet.jm.core.designsystem.util.shimmer
import org.koin.compose.getKoin
import org.koin.compose.viewmodel.koinActivityViewModel

/** 头图里小封面的宽度 */
private val HEADER_COVER_WIDTH = 112.dp

/** 简介收起时显示的行数 */
private const val DESCRIPTION_COLLAPSED_LINES = 4

/**
 * 大数字用中文习惯的「万」缩写。
 * 浏览量动辄五六位，原样铺开会把标题挤掉，也不好扫读。
 */
private fun formatCount(value: Int): String = when {
    value >= 10_000 -> {
        val wan = value / 10_000
        val decimal = (value % 10_000) / 1_000
        if (decimal == 0) "${wan}万" else "$wan.${decimal}万"
    }

    else -> value.toString()
}

/**
 * 头图背景的模糊处理。
 *
 * [Modifier.blur] 只在 API 31+ 生效，而本项目 minSdk 是 23。真正让低版本
 * 也有效果的是「把封面按 32px 请求再拉满」——放大本身就产生柔和色块，
 * 而且解码的是一张极小的图，内存代价几乎为零。31+ 再叠一层真模糊让边缘更顺。
 */
private fun Modifier.backdropBlur(): Modifier =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) this.blur(24.dp) else this

@Composable
private fun ComicStat(
    icon: ImageVector,
    label: String,
    value: Int,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 用 Box 而非 Chip 类组件：这里只是装饰，不应带有点击语义与无障碍角色
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(18.dp)
            )
        }
        Column {
            // 数值是主要信息，字号与字重都应高于标签
            Text(
                text = formatCount(value),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 详情页头图：模糊封面铺底 + 小封面 + 标题/作者/统计并排。
 *
 * 封面不铺满宽度：3:4 全宽会占掉约 70% 首屏，把标题、标签、按钮全推到折叠线以下。
 */
@Composable
private fun ComicDetailHeader(
    comic: Comic,
    onAuthorClick: (String) -> Unit,
    remoteSettingManager: RemoteSettingManager = getKoin().get(),
    imageLoader: ImageLoader = getKoin().get(),
) {
    val remoteSetting by remoteSettingManager.remoteSettingState.collectAsState()
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
private fun TagGroup(
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
private fun ComicDescription(description: String) {
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

@Composable
private fun ComicDetailSkeleton() {
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
private fun ComicDetailErrorPage(
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
private fun ComicDetailSections(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComicDetailScreen(
    id: Int,
    comicDetailViewModel: ComicDetailViewModel = koinActivityViewModel(),
    readHistoryManager: ReadHistoryManager = getKoin().get(),
    downloadManager: DownloadManager = getKoin().get(),
    userManager: UserManager = getKoin().get()
) {
    val mainNavController = LocalMainNavController.current
    val comicDetailState by comicDetailViewModel.comicDetailState.collectAsState()
    val readHistory by readHistoryManager.readHistoryState.collectAsState()
    val isLogin by userManager.isLoginState.collectAsState(false)
    var showDownloadChapterDialog by remember { mutableStateOf(false) }
    var selectedChapterIds by remember { mutableStateOf<Set<Int>>(emptySet()) }

    fun requireLogin(action: () -> Unit) {
        if (isLogin) action() else mainNavController.navigate("login")
    }

    fun searchTag(tag: String) {
        mainNavController.navigate("comicSearchResult/${Uri.encode(tag)}")
    }

    LaunchedEffect(id) {
        if (comicDetailState.data?.id != id) {
            comicDetailViewModel.getComicDetail(id)
        }
    }

    // Error state: no data and error occurred
    if (comicDetailState.isError && comicDetailState.data == null) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    navigationIcon = {
                        IconButton(onClick = { mainNavController.popBackStack() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回"
                            )
                        }
                    },
                    title = {}
                )
            }
        ) { innerPadding ->
            ComicDetailErrorPage(
                errorMessage = comicDetailState.errorMsg,
                onRetry = { comicDetailViewModel.getComicDetail(id) },
                onBack = { mainNavController.popBackStack() },
                modifier = Modifier.padding(innerPadding)
            )
        }
        return
    }

    if (comicDetailState.isLoading && comicDetailState.data == null) {
        ComicDetailSkeleton()
        return
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            val comicTitle = comicDetailState.data?.name ?: ""
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                ),
                navigationIcon = {
                    IconButton(onClick = { mainNavController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                title = {
                    Text(
                        text = comicTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            val comic = comicDetailState.data ?: return@Scaffold
            ComicDetailBottomBar(
                comic = comic,
                readHistoryManager = readHistoryManager,
                readHistory = readHistory,
                onLike = {
                    requireLogin {
                        if (!comic.isLike) comicDetailViewModel.likeComic(comic.id)
                    }
                },
                onCollect = {
                    requireLogin {
                        if (comic.isCollect) {
                            comicDetailViewModel.unCollect(comic.id)
                        } else if (comicDetailViewModel.shouldShowFolderPicker()) {
                            // 内置 API 模式：弹出收藏夹选择
                            comicDetailViewModel.refreshFolderList()
                            comicDetailViewModel.showFolderPicker()
                        } else {
                            // 网络 API 模式：直接收藏到默认夹
                            comicDetailViewModel.collect(comic.id)
                        }
                    }
                },
                onRelated = { mainNavController.navigate("comicRelate/${comic.id}") },
                onDownload = {
                    if (comic.comicChapterList.isEmpty()) {
                        downloadManager.downloadComic(comic)
                    } else {
                        selectedChapterIds = comic.comicChapterList.map { it.id }.toSet()
                        showDownloadChapterDialog = true
                    }
                },
                onRead = { targetId -> mainNavController.navigate("comicRead/$targetId") },
                onChapters = {
                    val currentChapterId = readHistoryManager.lastReadChapterId(comic, readHistory) ?: -1
                    mainNavController.navigate("comicChapter/${comic.id}?currentChapterId=$currentChapterId")
                }
            )
        }
    ) { innerPadding ->
        val comic = comicDetailState.data ?: return@Scaffold

        if (showDownloadChapterDialog) {
            ChapterMultiSelectDialog(
                title = "选择缓存章节",
                chapters = comic.comicChapterList,
                selectedChapterIds = selectedChapterIds,
                onSelectedChange = { selectedChapterIds = it },
                onDismiss = { showDownloadChapterDialog = false },
                confirmText = "开始缓存",
                onConfirm = {
                    val selectedChapters = comic.comicChapterList.filter { it.id in selectedChapterIds }
                    downloadManager.downloadChapters(comic, selectedChapters)
                    showDownloadChapterDialog = false
                }
            )
        }

        val openComments = { mainNavController.navigate("comment/${comic.id}") }

        PullToRefreshBox(
            isRefreshing = comicDetailState.isLoading,
            state = rememberPullToRefreshState(),
            onRefresh = { comicDetailViewModel.getComicDetail(id) },
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isTabletLayout = maxWidth >= 700.dp
                if (isTabletLayout) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        ComicCoverImage(
                            comic = comic,
                            modifier = Modifier
                                .widthIn(max = 320.dp)
                                .weight(0.42f),
                            showIdChip = true
                        )
                        // 用 LazyColumn 而非 verticalScroll：标签 chip 与长内容
                        // 不应在首屏一次性全部组合
                        LazyColumn(
                            modifier = Modifier.weight(0.58f),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            item {
                                Column(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = comic.name,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        comic.authorList.forEach {
                                            key(it) {
                                                Text(
                                                    modifier = Modifier.clickable { searchTag(it) },
                                                    text = it,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                                        ComicStat(Icons.Default.Favorite, "喜欢", comic.likeCount)
                                        ComicStat(Icons.Default.RemoveRedEye, "浏览", comic.readCount)
                                    }
                                }
                            }
                            item {
                                ComicDetailSections(comic, ::searchTag, openComments)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        item {
                            ComicDetailHeader(comic = comic, onAuthorClick = ::searchTag)
                        }
                        item {
                            ComicDetailSections(comic, ::searchTag, openComments)
                        }
                    }
                }
            }
        }

        // 收藏夹选择弹窗（仅内置 API 模式）
        val showFolderPicker by comicDetailViewModel.showFolderPicker.collectAsState()
        val folderList by comicDetailViewModel.folderList.collectAsState()
        if (showFolderPicker) {
            FolderPickerSheet(
                comicId = comic.id,
                folderList = folderList,
                onSelect = { folderId -> comicDetailViewModel.collectWithFolder(comic.id, folderId) },
                onDismiss = { comicDetailViewModel.hideFolderPicker() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderPickerSheet(
    comicId: Int,
    folderList: Map<String, String>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Text(
            text = "选择收藏夹",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        HorizontalDivider()
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
        ) {
            // "0"（全部）排第一，其余按原序
            val sortedFolders = linkedMapOf<String, String>().apply {
                folderList["0"]?.let { put("0", it) }
                folderList.filterKeys { it != "0" }.forEach { (id, name) -> put(id, name) }
                if (containsKey("0").not() && folderList.isNotEmpty()) {
                    put("0", "全部")
                }
            }
            items(sortedFolders.size) { index ->
                val entry = sortedFolders.entries.elementAt(index)
                ListItem(
                    headlineContent = { Text(entry.value) },
                    modifier = Modifier.clickable { onSelect(entry.key) }
                )
                if (index < sortedFolders.size - 1) {
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun ComicDetailBottomBar(
    comic: Comic,
    readHistoryManager: ReadHistoryManager,
    readHistory: Map<Int, ComicReadHistory>,
    onLike: () -> Unit,
    onCollect: () -> Unit,
    onRelated: () -> Unit,
    onDownload: () -> Unit,
    onRead: (Int) -> Unit,
    onChapters: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 3.dp,
        shadowElevation = 3.dp,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        val lastReadChapterId = readHistoryManager.lastReadChapterId(comic, readHistory)
        val readLabel = if (lastReadChapterId != null) "继续阅读" else "开始阅读"
        // 图标与主操作分两行。
        // 原先四个图标按钮与「章节」「开始阅读」挤在同一行，窄屏或放大字体时
        // 主按钮拿不到足够宽度，四个字会断成两行。分行后无论屏宽都能完整显示，
        // 主操作也拿到了整行的视觉权重。
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onLike) {
                    if (comic.isLike) {
                        Icon(Icons.Default.Favorite, contentDescription = "已喜欢", tint = MaterialTheme.colorScheme.error)
                    } else {
                        Icon(Icons.Default.FavoriteBorder, contentDescription = "喜欢")
                    }
                }
                IconButton(onClick = onCollect) {
                    if (comic.isCollect) {
                        Icon(Icons.Filled.Bookmark, contentDescription = "已收藏", tint = MaterialTheme.colorScheme.tertiary)
                    } else {
                        Icon(Icons.Filled.BookmarkBorder, contentDescription = "收藏")
                    }
                }
                IconButton(onClick = onRelated) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "相关")
                }
                IconButton(onClick = onDownload) {
                    Icon(Icons.Default.Download, contentDescription = "缓存")
                }
            }
            if (comic.comicChapterList.isEmpty()) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onRead(lastReadChapterId ?: comic.id) }
                ) {
                    Text(readLabel, maxLines = 1)
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // 「章节」是次要操作，用 Outlined 与主操作拉开层级，宽度也只占三分之一
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = onChapters
                    ) {
                        Text("章节", maxLines = 1)
                    }
                    Button(
                        modifier = Modifier.weight(2f),
                        onClick = {
                            val targetChapterId = lastReadChapterId
                                ?: comic.comicChapterList.firstOrNull()?.id
                                ?: comic.id
                            onRead(targetChapterId)
                        }
                    ) {
                        Text(readLabel, maxLines = 1)
                    }
                }
            }
        }
    }
}
