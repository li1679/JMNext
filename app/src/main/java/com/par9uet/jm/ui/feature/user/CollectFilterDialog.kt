package com.par9uet.jm.ui.feature.user

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.par9uet.jm.core.model.TagFilterLogic


// 筛选弹窗：ModalBottomSheet 支持上划全屏 + 逻辑门选择 + Tab（标签/作者）
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FilterDialog(
    tagCountMap: Map<String, Int>,
    authorCountMap: Map<String, Int>,
    draftSelectedTags: Set<String>,
    draftSelectedAuthors: Set<String>,
    draftTagLogic: TagFilterLogic,
    onTagToggle: (String) -> Unit,
    onAuthorToggle: (String) -> Unit,
    onTagLogicChange: (TagFilterLogic) -> Unit,
    onConfirm: () -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    // 弹窗内的搜索文本，用于过滤当前页的标签或作者
    var filterQuery by remember { mutableStateOf("") }
    // 打开即全屏
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // 根据搜索文本过滤当前页内容
    val query = filterQuery.trim()
    val filteredTagCountMap = remember(tagCountMap, query) {
        if (query.isBlank()) tagCountMap
        else tagCountMap.filterKeys { it.contains(query, ignoreCase = true) }
    }
    val filteredAuthorCountMap = remember(authorCountMap, query) {
        if (query.isBlank()) authorCountMap
        else authorCountMap.filterKeys { it.contains(query, ignoreCase = true) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "筛选收藏",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            // 逻辑门选择（仅对标签生效），放在筛选页最上面
            Text(
                text = "标签筛选逻辑",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TagFilterLogic.entries.forEach { logic ->
                    FilterChip(
                        selected = draftTagLogic == logic,
                        onClick = { onTagLogicChange(logic) },
                        label = {
                            Text(
                                text = logic.label,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            // 搜索框：搜索 tag 或作者
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = filterQuery,
                onValueChange = { filterQuery = it },
                singleLine = true,
                placeholder = {
                    Text(
                        if (selectedTabIndex == 0) "搜索标签" else "搜索作者"
                    )
                },
                leadingIcon = {
                    Icon(Icons.Rounded.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (filterQuery.isNotEmpty()) {
                        IconButton(onClick = { filterQuery = "" }) {
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
            Spacer(modifier = Modifier.height(12.dp))
            PrimaryTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = {
                        selectedTabIndex = 0
                        filterQuery = ""
                    },
                    text = { Text("标签 (${tagCountMap.size})") }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = {
                        selectedTabIndex = 1
                        filterQuery = ""
                    },
                    text = { Text("作者 (${authorCountMap.size})") }
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (selectedTabIndex) {
                    0 -> {
                        if (filteredTagCountMap.isEmpty()) {
                            Text(
                                if (tagCountMap.isEmpty()) "当前已加载收藏中没有可筛选的标签"
                                else "没有匹配「$query」的标签",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 24.dp)
                            )
                        } else {
                            FlowRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                filteredTagCountMap.forEach { (tag, count) ->
                                    FilterChip(
                                        selected = tag in draftSelectedTags,
                                        onClick = { onTagToggle(tag) },
                                        label = { Text("$tag  $count") }
                                    )
                                }
                            }
                        }
                    }
                    1 -> {
                        if (filteredAuthorCountMap.isEmpty()) {
                            Text(
                                if (authorCountMap.isEmpty()) "当前已加载收藏中没有可筛选的作者"
                                else "没有匹配「$query」的作者",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 24.dp)
                            )
                        } else {
                            FlowRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                filteredAuthorCountMap.forEach { (author, count) ->
                                    FilterChip(
                                        selected = author in draftSelectedAuthors,
                                        onClick = { onAuthorToggle(author) },
                                        label = { Text("$author  $count") }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            // 底部操作栏（固定不随内容滚动消失）
            Surface(
                tonalElevation = 3.dp,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onClear,
                        modifier = Modifier.weight(1f)
                    ) { Text("清空") }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f)
                    ) { Text("确定") }
                }
            }
        }
    }
}

