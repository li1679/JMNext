package com.par9uet.jm.ui.feature.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.par9uet.jm.data.storage.LocalSettingManager
import com.par9uet.jm.ui.component.Comic
import com.par9uet.jm.ui.component.CommonScaffold
import com.par9uet.jm.ui.component.adaptiveComicGridCells
import com.par9uet.jm.ui.feature.detail.ComicDetailViewModel
import com.par9uet.jm.core.common.filterBlockedTags
import org.koin.compose.getKoin
import org.koin.compose.viewmodel.koinActivityViewModel

@Composable
fun ComicRelateListScreen(
    comicId: Int = -1,
    comicDetailViewModel: ComicDetailViewModel = koinActivityViewModel(),
    localSettingManager: LocalSettingManager = getKoin().get(),
) {
    val comicDetailState by comicDetailViewModel.comicDetailState.collectAsStateWithLifecycle()
    val localSetting by localSettingManager.localSettingState.collectAsStateWithLifecycle()
    // 详情 ViewModel 是 Activity 级共享的，从 A 的相关列表打开 B 之后它装的就是 B。
    // 以路由携带的 id 为准重新拉取，否则返回 A 时会看到 B 的相关推荐；
    // 进程被回收后直接恢复到本路由时，也能靠它把数据补回来。
    LaunchedEffect(comicId) {
        if (comicId > 0 && comicDetailState.data?.id != comicId) {
            comicDetailViewModel.getComicDetail(comicId)
        }
    }
    CommonScaffold(title = "相关本子") {
        if (comicDetailState.data != null) {
            val relateList = remember(comicDetailState.data, localSetting.blockedTagList) {
                comicDetailState.data?.relateComicList?.filterBlockedTags(localSetting.blockedTagList) ?: emptyList()
            }
            LazyVerticalGrid(
                columns = adaptiveComicGridCells(),
                verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Top),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.Start),
                contentPadding = PaddingValues(10.dp),
            ) {
                items(
                    relateList,
                    key = { it.id },
                ) {
                    Comic(comic = it)
                }
            }
        }
    }
}
