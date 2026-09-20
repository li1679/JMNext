package com.par9uet.jm.ui.feature.category

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.par9uet.jm.core.common.filterBlockedTags
import com.par9uet.jm.core.model.CategoryFilter
import com.par9uet.jm.core.model.Comic
import com.par9uet.jm.data.network.model.NetWorkResult
import com.par9uet.jm.data.repository.ComicRepository

class CategoryComicPagingSource(
    private val repository: ComicRepository,
    private val filter: CategoryFilter,
    private val blockedTags: List<String>,
) : PagingSource<Int, Comic>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Comic> {
        val page = params.key ?: 1
        return when (val result = repository.getCategoryComics(page, filter)) {
            is NetWorkResult.Error -> LoadResult.Error(IllegalStateException(result.message))
            is NetWorkResult.Success -> LoadResult.Page(
                data = result.data.comics.filterBlockedTags(blockedTags),
                prevKey = if (page > 1) page - 1 else null,
                nextKey = result.data.nextPage,
            )
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Comic>): Int? = null
}
