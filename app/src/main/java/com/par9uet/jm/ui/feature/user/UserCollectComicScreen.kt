package com.par9uet.jm.ui.feature.user

import com.par9uet.jm.navigation.LocalMainNavController

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.DriveFileMove
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.par9uet.jm.core.model.CollectComicOrderFilter
import com.par9uet.jm.core.model.TagFilterLogic
import com.par9uet.jm.ui.component.Comic
import com.par9uet.jm.ui.component.ComicSkeleton
import com.par9uet.jm.ui.component.PullRefreshAndLoadMoreGrid
import com.par9uet.jm.ui.component.adaptiveComicGridCells
import com.par9uet.jm.ui.feature.user.UserViewModel
import com.par9uet.jm.data.storage.LocalSettingManager
import org.koin.compose.getKoin
import org.koin.compose.viewmodel.koinActivityViewModel

// 收藏列表加载中的骨架屏，2 列布局与正式网格保持一致
@Composable
private fun UserCollectComicSkeleton(
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(14.dp)
            .verticalScroll(rememberScrollState()),
        maxItemsInEachRow = 2,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.Top)
    ) {
        for (i in 0 until 8) {
            key(i) {
                ComicSkeleton(
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// 收藏夹切换 Chip
@Composable
private fun FolderChip(
    folderName: String,
    isSelected: Boolean,
    isAll: Boolean = false,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(folderName) },
        leadingIcon = {
            Icon(
                imageVector = if (isAll) Icons.Rounded.Bookmarks else Icons.Rounded.Folder,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserCollectComicScreen(
    userViewModel: UserViewModel = koinActivityViewModel(),
    useScaffold: Boolean = true,
    localSettingManager: LocalSettingManager = getKoin().get(),
) {
    val navController = LocalMainNavController.current
    val collectComicLazyPagingItems = userViewModel.collectComicPager.collectAsLazyPagingItems()
    val order by userViewModel.collectComicOrder.collectAsStateWithLifecycle()
    val collectComicFilter by userViewModel.collectComicFilter.collectAsStateWithLifecycle()
    val tagCountMap by userViewModel.collectTagCounts.collectAsStateWithLifecycle()
    val authorCountMap by userViewModel.collectAuthorCounts.collectAsStateWithLifecycle()
    val selectedFolderId by userViewModel.selectedFolderId.collectAsStateWithLifecycle()
    val folderList by userViewModel.folderList.collectAsStateWithLifecycle()
    val collectEditState by userViewModel.collectEditState.collectAsStateWithLifecycle()
    val localSetting by localSettingManager.localSettingState.collectAsStateWithLifecycle()
    var draftSelectedTags by remember { mutableStateOf<Set<String>>(emptySet()) }
    var draftSelectedAuthors by remember { mutableStateOf<Set<String>>(emptySet()) }
    var draftTagLogic by remember { mutableStateOf(TagFilterLogic.AND) }
    var showFilterDialog by remember { mutableStateOf(false) }

    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var actionFolderId by remember { mutableStateOf<String?>(null) }
    var actionFolderName by remember { mutableStateOf("") }
    var showFolderManageSheet by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameFolderName by remember { mutableStateOf("") }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showMoveFolderDialog by remember { mutableStateOf(false) }
    var showDeleteCollectConfirmDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = collectEditState.editing) {
        userViewModel.clearCollectSelection()
    }
    DisposableEffect(userViewModel) {
        onDispose { userViewModel.clearCollectSelection() }
    }

    val folders = remember(folderList) {
        val result = linkedMapOf<String, String>()
        result["0"] = folderList["0"] ?: "全部"
        folderList.filterKeys { it != "0" }.forEach { (id, name) -> result[id] = name }
        result
    }

    val selectedComics: List<com.par9uet.jm.core.model.Comic> = remember(collectComicLazyPagingItems.itemSnapshotList, collectEditState.selectedComicIds) {
        collectComicLazyPagingItems.itemSnapshotList.filterNotNull().filter { it.id in collectEditState.selectedComicIds }
    }

    // 当前激活的筛选项数量，用于在筛选按钮上展示
    val activeFilterCount = collectComicFilter.selectedTags.size + collectComicFilter.selectedAuthors.size

    // 只在首次进入时统计；排序、收藏夹切换与收藏变更会各自触发重算
    LaunchedEffect(Unit) {
        userViewModel.ensureCollectMeta()
    }

    val mainContent: @Composable () -> Unit = {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = collectComicFilter.searchText,
                    onValueChange = { userViewModel.updateCollectSearchText(it) },
                    singleLine = true,
                    placeholder = { Text("搜索漫画名 / 作者 / 标签") },
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (collectComicFilter.searchText.isNotEmpty()) {
                            IconButton(onClick = { userViewModel.updateCollectSearchText("") }) {
                                Icon(Icons.Rounded.Close, contentDescription = "清除")
                            }
                        }
                    },
                    shape = MaterialTheme.shapes.large,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
                IconButton(
                    onClick = {
                        draftSelectedTags = collectComicFilter.selectedTags
                        draftSelectedAuthors = collectComicFilter.selectedAuthors
                        draftTagLogic = collectComicFilter.tagLogic
                        showFilterDialog = true
                    },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        Icons.Rounded.FilterList,
                        contentDescription = "筛选",
                        modifier = Modifier.size(22.dp),
                        tint = if (activeFilterCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 收藏夹切换栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                folders.forEach { (folderId, folderName) ->
                    key(folderId) {
                        FolderChip(
                            folderName = folderName,
                            isSelected = selectedFolderId == folderId.toIntOrNull(),
                            isAll = folderId == "0",
                            onClick = {
                                userViewModel.changeFolder(folderId.toIntOrNull() ?: 0)
                            }
                        )
                    }
                }
                IconButton(onClick = {
                    newFolderName = ""
                    showCreateFolderDialog = true
                }) {
                    Icon(Icons.Rounded.Add, contentDescription = "新建收藏夹", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = { showFolderManageSheet = true }) {
                    Icon(Icons.Rounded.Folder, contentDescription = "管理收藏夹", modifier = Modifier.size(20.dp))
                }
            }

            // 排序：Material 3 单选 SegmentedButton
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp)
            ) {
                CollectComicOrderFilter.entries.forEachIndexed { index, item ->
                    SegmentedButton(
                        selected = item == order,
                        onClick = { userViewModel.changeCollectComicOrder(item) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index,
                            CollectComicOrderFilter.entries.size
                        )
                    ) {
                        Text(item.label)
                    }
                }
            }

            HorizontalDivider()

            // 漫画列表：2 列网格，间距更大
            if (collectComicLazyPagingItems.loadState.refresh is LoadState.Loading && collectComicLazyPagingItems.itemCount == 0) {
                UserCollectComicSkeleton(
                    modifier = Modifier.weight(1f)
                )
            } else {
                PullRefreshAndLoadMoreGrid(
                    modifier = Modifier.weight(1f),
                    lazyPagingItems = collectComicLazyPagingItems,
                    key = { it.id },
                    columns = adaptiveComicGridCells(localSetting.collectGridColumns),
                    verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.Top),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(14.dp)
                ) { comic ->
                    Comic(
                        comic = comic,
                        editing = collectEditState.editing,
                        selected = comic.id in collectEditState.selectedComicIds,
                        onLongClick = {
                            if (collectEditState.editing) {
                                userViewModel.toggleCollectSelected(comic.id)
                            } else {
                                userViewModel.enterCollectEdit(comic.id)
                            }
                        },
                        onToggleSelected = {
                            userViewModel.toggleCollectSelected(comic.id)
                        },
                        onBeforeOpenDetail = {
                            userViewModel.clearCollectSelection()
                        }
                    )
                }
            }
        }
    }

    // 编辑模式下的底部工具栏
    val editBar: @Composable () -> Unit = {
        if (collectEditState.editing) {
            CollectEditBottomAppBar(
                selectedCount = collectEditState.selectedComicIds.size,
                onClose = userViewModel::clearCollectSelection,
                onCache = { userViewModel.cacheCollectedComics(selectedComics) },
                onDelete = { showDeleteCollectConfirmDialog = true },
                onMove = { showMoveFolderDialog = true }
            )
        }
    }

    if (useScaffold) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "我的收藏",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )
            },
            bottomBar = { editBar() }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                mainContent()
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                mainContent()
            }
            editBar()
        }
    }

    if (showFilterDialog) {
        FilterDialog(
            tagCountMap = tagCountMap,
            authorCountMap = authorCountMap,
            draftSelectedTags = draftSelectedTags,
            draftSelectedAuthors = draftSelectedAuthors,
            draftTagLogic = draftTagLogic,
            onTagToggle = { tag ->
                draftSelectedTags = if (tag in draftSelectedTags) {
                    draftSelectedTags - tag
                } else {
                    draftSelectedTags + tag
                }
            },
            onAuthorToggle = { author ->
                draftSelectedAuthors = if (author in draftSelectedAuthors) {
                    draftSelectedAuthors - author
                } else {
                    draftSelectedAuthors + author
                }
            },
            onTagLogicChange = { draftTagLogic = it },
            onConfirm = {
                userViewModel.updateCollectSelectedTags(draftSelectedTags)
                userViewModel.updateCollectSelectedAuthors(draftSelectedAuthors)
                userViewModel.updateCollectTagLogic(draftTagLogic)
                showFilterDialog = false
            },
            onClear = {
                draftSelectedTags = emptySet()
                draftSelectedAuthors = emptySet()
                draftTagLogic = TagFilterLogic.AND
                userViewModel.updateCollectSelectedTags(emptySet())
                userViewModel.updateCollectSelectedAuthors(emptySet())
                userViewModel.updateCollectTagLogic(TagFilterLogic.AND)
                showFilterDialog = false
            },
            onDismiss = { showFilterDialog = false }
        )
    }

    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = {
                showCreateFolderDialog = false
                newFolderName = ""
            },
            title = { Text("新建收藏夹") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    singleLine = true,
                    label = { Text("文件夹名称") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newFolderName.isNotBlank()) {
                        userViewModel.createFolder(newFolderName.trim())
                        newFolderName = ""
                        showCreateFolderDialog = false
                    }
                }) { Text("创建") }
            },
            dismissButton = {
                TextButton(onClick = {
                    newFolderName = ""
                    showCreateFolderDialog = false
                }) { Text("取消") }
            }
        )
    }

    if (showFolderManageSheet) {
        val manageSheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showFolderManageSheet = false },
            sheetState = manageSheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    "管理收藏夹",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "点击文件夹名称可切换当前收藏夹，右侧按钮可重命名或删除。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(folders.entries.toList(), key = { it.key }) { (folderId, folderName) ->
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = if (selectedFolderId == folderId.toIntOrNull())
                                MaterialTheme.colorScheme.secondaryContainer
                            else androidx.compose.ui.graphics.Color.Transparent,
                            onClick = {
                                userViewModel.changeFolder(folderId.toIntOrNull() ?: 0)
                                showFolderManageSheet = false
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (folderId == "0") Icons.Rounded.Bookmarks else Icons.Rounded.Folder,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    folderName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                if (folderId != "0") {
                                    IconButton(onClick = {
                                        actionFolderId = folderId
                                        actionFolderName = folderName
                                        renameFolderName = folderName
                                        showRenameDialog = true
                                    }) {
                                        Icon(Icons.Rounded.Edit, contentDescription = "重命名")
                                    }
                                    IconButton(onClick = {
                                        actionFolderId = folderId
                                        actionFolderName = folderName
                                        showDeleteConfirmDialog = true
                                    }) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "删除")
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        newFolderName = ""
                        showCreateFolderDialog = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("新建收藏夹")
                }
            }
        }
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("重命名收藏夹") },
            text = {
                OutlinedTextField(
                    value = renameFolderName,
                    onValueChange = { renameFolderName = it },
                    singleLine = true,
                    label = { Text("新名称") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val folderId = actionFolderId
                    if (renameFolderName.isNotBlank() && folderId != null) {
                        userViewModel.renameFolder(folderId, renameFolderName.trim())
                        showRenameDialog = false
                    }
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("取消") }
            }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("删除收藏夹") },
            text = { Text("确定删除「${actionFolderName}」吗？\n注意：删除收藏夹不会删除其中的漫画，漫画会移至「全部」。") },
            confirmButton = {
                TextButton(onClick = {
                    val folderId = actionFolderId
                    if (folderId != null) {
                        userViewModel.deleteFolder(folderId)
                        showDeleteConfirmDialog = false
                    }
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("取消") }
            }
        )
    }

    if (showDeleteCollectConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteCollectConfirmDialog = false },
            title = { Text("取消收藏") },
            text = { Text("确定取消收藏 ${collectEditState.selectedComicIds.size} 部漫画吗？") },
            confirmButton = {
                TextButton(onClick = {
                    userViewModel.deleteCollectedComics(selectedComics)
                    showDeleteCollectConfirmDialog = false
                }) { Text("取消收藏") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteCollectConfirmDialog = false }) { Text("取消") }
            }
        )
    }

    if (showMoveFolderDialog) {
        MoveFolderSheet(
            folders = folders,
            currentFolderId = selectedFolderId,
            onMove = { folderId ->
                userViewModel.moveCollectedToFolder(selectedComics, folderId)
                showMoveFolderDialog = false
            },
            onDismiss = { showMoveFolderDialog = false }
        )
    }
}

