package com.par9uet.jm.ui.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.par9uet.jm.core.model.ComicSearchOrderFilter
import com.par9uet.jm.core.model.HomeComicSwiperItem
import com.par9uet.jm.core.model.WeekData
import com.par9uet.jm.data.repository.ComicRepository
import com.par9uet.jm.data.network.model.HomeSwiperComicListItemResponse
import com.par9uet.jm.data.network.model.NetWorkResult
import com.par9uet.jm.data.network.model.WeekResponse
import com.par9uet.jm.data.storage.LocalSettingManager
import com.par9uet.jm.core.model.CommonUIState
import com.par9uet.jm.ui.feature.search.SearchComicFilter
import com.par9uet.jm.ui.feature.search.SearchComicPagingSource
import com.par9uet.jm.ui.feature.home.WeekComicPagingSource
import com.par9uet.jm.ui.feature.home.WeekFilter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ComicViewModel(
    private val comicRepository: ComicRepository,
    private val localSettingManager: LocalSettingManager,
) : ViewModel() {
    data class HomeComicUIState(
        val isLoading: Boolean = true,
        val isError: Boolean = false,
        val list: List<HomeComicSwiperItem> = listOf(),
        val errorMsg: String? = null
    )

    private val _homeComicState = MutableStateFlow(HomeComicUIState())
    val homeComicState = _homeComicState.asStateFlow()

    /** 已成功加载过首页数据时所用的数据源，null 表示尚未加载成功 */
    private var loadedApiSource: String? = null

    /**
     * 按需加载。首页是 Tab 页，导航返回时 NavHost 会重建 composable 并重跑 LaunchedEffect，
     * 在那里直接请求会让每次退回首页都整页重刷。仅在未加载成功或数据源变更时发请求，
     * 主动刷新走 [refreshHomeComic]。
     */
    fun ensureHomeComic(apiSource: String) {
        if (loadedApiSource == apiSource && _homeComicState.value.list.isNotEmpty()) return
        loadHomeComic(apiSource)
    }

    /** 下拉刷新：无条件重新请求 */
    fun refreshHomeComic() {
        loadHomeComic(localSettingManager.localSettingState.value.comicApiSource)
    }

    private fun loadHomeComic(apiSource: String) {
        viewModelScope.launch {
            _homeComicState.update {
                it.copy(
                    isLoading = true,
                    isError = false,
                    errorMsg = ""
                )
            }
            when (val data = comicRepository.getHomeSwiperComicList()) {
                is NetWorkResult.Error -> {
                    _homeComicState.update {
                        it.copy(isError = true, errorMsg = data.message)
                    }
                }

                is NetWorkResult.Success<List<HomeSwiperComicListItemResponse>> -> {
                    // 只有成功才记录数据源，失败时下次进入应当重试
                    loadedApiSource = apiSource
                    _homeComicState.update {
                        it.copy(list = data.data.map { item -> item.toHomeComicSwiperItem() })
                    }
                }
            }
            _homeComicState.update {
                it.copy(isLoading = false)
            }
        }
    }

    private val _searchComicFilterState = MutableStateFlow(SearchComicFilter())
    val searchComicFilterState = _searchComicFilterState.asStateFlow()
    private val _searchComicIdState = MutableStateFlow<Int?>(null)
    val searchComicIdState = _searchComicIdState.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val searchComicPager = combine(
        _searchComicFilterState,
        localSettingManager.localSettingState
    ) { filter, localSetting -> filter to localSetting.blockedTagList }
        .flatMapLatest { (filter, blockedTagList) ->
        Pager(
            config = PagingConfig(
                pageSize = 20,
                prefetchDistance = 6,
                initialLoadSize = 20
            ),
            pagingSourceFactory = {
                SearchComicPagingSource(
                    comicRepository,
                    filter.copy(excludedTags = (filter.excludedTags + blockedTagList).distinct()),
                ) { id ->
                    _searchComicIdState.update {
                        id
                    }
                }
            }
        ).flow
    }.cachedIn(viewModelScope)

    fun changeSearchComicOrderFilter(order: ComicSearchOrderFilter) {
        _searchComicIdState.update { null }
        _searchComicFilterState.update {
            it.copy(
                order = order
            )
        }
    }

    fun changeSearchComicContent(searchContent: String) {
        _searchComicIdState.update { null }
        _searchComicFilterState.update {
            it.copy(
                searchContent = searchContent
            )
        }
    }

    fun changeSearchComicContent(searchContent: String, excludedTags: List<String>) {
        _searchComicIdState.update { null }
        _searchComicFilterState.update {
            it.copy(
                searchContent = searchContent,
                excludedTags = excludedTags
            )
        }
    }

    fun consumeSearchComicId() {
        _searchComicIdState.update { null }
    }

    private val _weekDataState = MutableStateFlow(CommonUIState<WeekData>())
    val weekDataState = _weekDataState.asStateFlow()
    fun getWeekData() {
        viewModelScope.launch {
            _weekDataState.update {
                it.copy(
                    isLoading = true,
                    isError = false,
                    errorMsg = ""
                )
            }
            when (val data = comicRepository.getWeekData()) {
                is NetWorkResult.Error -> {
                    _weekDataState.update {
                        it.copy(isError = true, errorMsg = data.message)
                    }
                }

                is NetWorkResult.Success<WeekResponse> -> {
                    val d = data.data.toWeekData()
                    _weekDataState.update {
                        it.copy(data = d)
                    }
                    if (d.categoryList.isNotEmpty()) {
                        _weekFilterState.update {
                            it.copy(categoryId = d.categoryList[0].first)
                        }
                    }
                    if (d.typeList.isNotEmpty()) {
                        _weekFilterState.update {
                            it.copy(typeId = d.typeList[0].first)
                        }
                    }
                }
            }
            _weekDataState.update {
                it.copy(isLoading = false)
            }
        }
    }

    private val _weekFilterState = MutableStateFlow(WeekFilter())
    val weekFilterState = _weekFilterState.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val weekComicPager = combine(
        _weekFilterState,
        localSettingManager.localSettingState
    ) { filter, localSetting ->
        filter to (localSetting.blockedTagList + localSetting.homeExcludedTags).distinct()
    }
        .flatMapLatest { (filter, blockedTagList) ->
        Pager(
            config = PagingConfig(
                pageSize = 20,
                prefetchDistance = 6,
                initialLoadSize = 20
            ),
            pagingSourceFactory = {
                WeekComicPagingSource(
                    comicRepository,
                    filter,
                    blockedTagList
                )
            }
        ).flow
    }.cachedIn(viewModelScope)

    fun changeWeekCategoryFilter(categoryId: String?) {
        _weekFilterState.update {
            it.copy(
                categoryId = categoryId
            )
        }
    }

    fun changeWeekTypeFilter(typeId: String?) {
        _weekFilterState.update {
            it.copy(
                typeId = typeId
            )
        }
    }
}
