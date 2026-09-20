package com.par9uet.jm.ui.feature.user

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.par9uet.jm.core.model.Comic
import com.par9uet.jm.data.repository.UserRepository
import com.par9uet.jm.data.network.model.NetWorkResult
import com.par9uet.jm.data.network.model.UserHistoryComicListResponse

class HistoryComicPagingSource(
    private val userRepository: UserRepository,
) : PagingSource<Int, Comic>() {
    private val loadedIdsByPage = mutableMapOf<Int, Set<Int>>()

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Comic> {
        val currentPage = params.key ?: 1
        if (params is LoadParams.Refresh) loadedIdsByPage.clear()
        return when (val data =
            userRepository.getHistoryComicList(currentPage)) {
            is NetWorkResult.Error -> {
                LoadResult.Error(Exception(data.message))
            }

            is NetWorkResult.Success<UserHistoryComicListResponse> -> {
                val loadedIds = loadedIdsByPage.filterKeys { it < currentPage }.values.flatten().toSet()
                val rawList = data.data.toComicList().distinctBy { it.id }
                val newItems = rawList.filterNot { it.id in loadedIds }
                loadedIdsByPage[currentPage] = rawList.mapTo(mutableSetOf()) { it.id }
                LoadResult.Page(
                    data = newItems,
                    prevKey = null,
                    nextKey = if (newItems.isEmpty()) null else currentPage + 1
                )
            }
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Comic>): Int? = null
}
