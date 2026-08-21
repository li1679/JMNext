package com.par9uet.jm.ui.feature.search

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.par9uet.jm.core.model.Comic
import com.par9uet.jm.core.model.ComicSearchOrderFilter
import com.par9uet.jm.data.repository.ComicRepository
import com.par9uet.jm.data.network.model.ComicDetailResponse
import com.par9uet.jm.data.network.model.ComicListResponse
import com.par9uet.jm.data.network.model.NetWorkResult
import com.par9uet.jm.core.common.filterBlockedTags
import com.par9uet.jm.core.common.normalizeSearchExcludedTags
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

data class SearchComicFilter(
    val order: ComicSearchOrderFilter = ComicSearchOrderFilter.NEWEST,
    val searchContent: String = "",
    val excludedTags: List<String> = emptyList(),
)

class SearchComicPagingSource(
    private val comicRepository: ComicRepository,
    private val filter: SearchComicFilter,
    private val onFindSingleComicId: (id: Int?) -> Unit = {}
) : PagingSource<Int, Comic>() {
    companion object {
        private const val DETAIL_FILTER_BATCH_SIZE = 6
    }

    private val detailBlockedCache = mutableMapOf<Int, Boolean>()

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Comic> {
        val currentPage = params.key ?: 1
        val excludedTags = normalizeSearchExcludedTags(filter.excludedTags)
        val searchQuery = buildSearchQuery(filter.searchContent, excludedTags)
        return when (val data =
            comicRepository.getComicList(currentPage, filter.order, searchQuery)) {
            is NetWorkResult.Error -> {
                LoadResult.Error(Exception(data.message))
            }

            is NetWorkResult.Success<ComicListResponse> -> {
                // 取局部变量：跨模块的 public 属性无法做智能转换
                val redirectAid = data.data.redirect_aid
                if (redirectAid != null) {
                    val redirectId = redirectAid.toInt()
                    if (isComicBlockedByDetail(redirectId, excludedTags.toTagSet())) {
                        onFindSingleComicId(null)
                    } else {
                        onFindSingleComicId(redirectId)
                    }
                    LoadResult.Page(
                        data = listOf(),
                        prevKey = null,
                        nextKey = null
                    )
                } else {
                    onFindSingleComicId(null)
                    val list = filterExcludedComics(data.data.toComicList(), excludedTags)
                    // 用过滤前的原始条数判断是否末页：排除标签过滤后本页可能所剩无几，
                    // 若据此判断会误以为到底；内置数据源的 total 也不是总条数
                    val isLastPage = data.data.content.size < params.loadSize
                    LoadResult.Page(
                        data = list,
                        prevKey = if (currentPage == 1) null else currentPage - 1,
                        nextKey = if (isLastPage) null else currentPage + 1
                    )
                }
            }
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Comic>): Int? = null

    private suspend fun filterExcludedComics(
        candidates: List<Comic>,
        excludedTags: List<String>
    ): List<Comic> {
        if (candidates.isEmpty() || excludedTags.isEmpty()) return candidates

        val list = candidates.filterBlockedTags(excludedTags)
        if (excludedTags.size <= 1 || list.isEmpty()) return list

        return filterByDetailTags(list, excludedTags.toTagSet())
    }

    private suspend fun filterByDetailTags(
        candidates: List<Comic>,
        excludedTagSet: Set<String>
    ): List<Comic> {
        val result = mutableListOf<Comic>()
        candidates.chunked(DETAIL_FILTER_BATCH_SIZE).forEach { chunk ->
            val checkedChunk = coroutineScope {
                chunk.map { comic ->
                    async {
                        comic to isComicBlockedByDetail(comic.id, excludedTagSet)
                    }
                }.awaitAll()
            }
            result += checkedChunk
                .filterNot { (_, isBlocked) -> isBlocked }
                .map { (comic, _) -> comic }
        }
        return result
    }

    private suspend fun isComicBlockedByDetail(
        comicId: Int,
        excludedTagSet: Set<String>
    ): Boolean {
        if (excludedTagSet.isEmpty()) return false
        detailBlockedCache[comicId]?.let { return it }

        val detail = comicRepository.getComicDetail(comicId)
        if (detail !is NetWorkResult.Success<ComicDetailResponse>) {
            // 请求失败只影响本次判定，不写缓存：
            // 一次网络抖动若被记成「不需要排除」，这本漫画在整个搜索会话里
            // 都会绕过标签排除，而用户看不出原因
            return false
        }
        val isBlocked = detail.data.containsAnyExcludedTag(excludedTagSet)
        detailBlockedCache[comicId] = isBlocked
        return isBlocked
    }

    private fun buildSearchQuery(searchContent: String, excludedTags: List<String>): String {
        val baseQuery = searchContent.trim().replace(Regex("\\s+"), " ")
        val excludedQuery = excludedTags.joinToString(" ") { tag -> "-$tag" }
        return listOf(baseQuery, excludedQuery)
            .filter { it.isNotBlank() }
            .joinToString(" ")
    }

    private fun List<String>.toTagSet(): Set<String> {
        return normalizeSearchExcludedTags(this).map { it.toTagKey() }.toSet()
    }

    private fun ComicDetailResponse.containsAnyExcludedTag(excludedTagSet: Set<String>): Boolean {
        return (tags + actors + works)
            .map { it.toTagKey() }
            .any { it in excludedTagSet }
    }

    private fun String.toTagKey(): String {
        return trim().lowercase()
    }
}
