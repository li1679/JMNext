package com.par9uet.jm.ui.feature.search

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.par9uet.jm.core.common.normalizeSearchExcludedTags
import com.par9uet.jm.core.model.Comic
import com.par9uet.jm.core.model.ComicSearchOrderFilter
import com.par9uet.jm.data.network.model.NetWorkResult
import com.par9uet.jm.data.repository.ComicRepository
import com.par9uet.jm.data.repository.ComicTagFilter

data class SearchComicFilter(
    val order: ComicSearchOrderFilter = ComicSearchOrderFilter.NEWEST,
    val searchContent: String = "",
    val excludedTags: List<String> = emptyList(),
)

class SearchComicPagingSource(
    private val comicRepository: ComicRepository,
    private val filter: SearchComicFilter,
    private val tagFilter: ComicTagFilter = ComicTagFilter(),
    private val onFindSingleComicId: (id: Int?) -> Unit = {},
) : PagingSource<Int, Comic>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Comic> {
        val page = params.key ?: 1
        val excludedTags = normalizeSearchExcludedTags(filter.excludedTags)
        val query = listOf(
            filter.searchContent.trim().replace(Regex("\\s+"), " "),
            excludedTags.joinToString(" ") { "-$it" },
        ).filter { it.isNotBlank() }.joinToString(" ")
        return when (val response = comicRepository.getComicList(page, filter.order, query)) {
            is NetWorkResult.Error -> LoadResult.Error(IllegalStateException(response.message))
            is NetWorkResult.Success -> {
                val data = response.data
                val redirectId = data.redirect_aid?.toIntOrNull()
                if (data.redirect_aid != null && redirectId == null) {
                    return LoadResult.Error(IllegalStateException("数据源返回无效作品编号"))
                }
                val candidates = if (redirectId != null) listOf(Comic.create(redirectId, "", emptyList()))
                    else data.toComicList()
                when (val filtered = tagFilter.filter(candidates, excludedTags)) {
                    is NetWorkResult.Error -> LoadResult.Error(IllegalStateException(filtered.message))
                    is NetWorkResult.Success -> {
                        if (invalid) return LoadResult.Invalid()
                        onFindSingleComicId(redirectId?.takeIf { filtered.data.isNotEmpty() })
                        LoadResult.Page(
                            data = if (redirectId != null) emptyList() else filtered.data,
                            prevKey = if (redirectId != null || page == 1) null else page - 1,
                            nextKey = if (redirectId != null || data.content.isEmpty()) null else data.nextPage,
                        )
                    }
                }
            }
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Comic>): Int? = null
}
