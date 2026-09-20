package com.par9uet.jm.ui.feature.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.par9uet.jm.core.model.CategoryFilter
import com.par9uet.jm.core.model.CategoryOrder
import com.par9uet.jm.core.model.ComicCategory
import com.par9uet.jm.data.network.model.NetWorkResult
import com.par9uet.jm.data.repository.ComicRepository
import com.par9uet.jm.data.repository.ComicTagFilter
import com.par9uet.jm.data.storage.LocalSettingManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CategoryDirectoryState(
    val loading: Boolean = true,
    val categories: List<ComicCategory> = emptyList(),
    val error: String? = null,
)

class CategoryViewModel(
    private val repository: ComicRepository,
    settings: LocalSettingManager,
    private val tagFilter: ComicTagFilter,
) : ViewModel() {
    private val _directory = MutableStateFlow(CategoryDirectoryState())
    val directory = _directory.asStateFlow()
    private val _filter = MutableStateFlow(CategoryFilter())
    val filter = _filter.asStateFlow()
    private var directoryJob: Job? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    val comics = combine(
        filter,
        settings.localSettingState.map { it.globalExcludedTags }.distinctUntilChanged(),
    ) { filter, tags -> filter to tags }
        .flatMapLatest { (filter, tags) ->
            Pager(PagingConfig(pageSize = 80, initialLoadSize = 80, prefetchDistance = 6)) {
                CategoryComicPagingSource(repository, filter, tags, tagFilter)
            }.flow
        }.cachedIn(viewModelScope)

    init { loadDirectory() }

    fun loadDirectory() {
        if (directoryJob?.isActive == true) return
        directoryJob = viewModelScope.launch {
            _directory.update { it.copy(loading = true, error = null) }
            when (val result = repository.getCategories()) {
                is NetWorkResult.Error -> _directory.update {
                    it.copy(loading = false, error = result.message)
                }
                is NetWorkResult.Success -> _directory.value = CategoryDirectoryState(
                    loading = false, categories = result.data,
                )
            }
        }
    }

    fun selectCategory(slug: String) {
        val category = directory.value.categories.firstOrNull { it.slug == slug }
        _filter.update {
            if (it.categorySlug == slug) it else it.copy(
                categorySlug = slug, subCategorySlug = null, search = category?.search == true,
            )
        }
    }

    fun selectSubCategory(slug: String?) { _filter.update { it.copy(subCategorySlug = slug) } }
    fun selectOrder(order: CategoryOrder) { _filter.update { it.copy(order = order) } }
}
