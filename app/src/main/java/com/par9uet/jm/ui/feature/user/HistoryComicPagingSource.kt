package com.par9uet.jm.ui.feature.user

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.par9uet.jm.core.model.Comic
import com.par9uet.jm.data.repository.UserRepository
import com.par9uet.jm.data.network.model.NetWorkResult
import com.par9uet.jm.data.network.model.UserHistoryComicListResponse
import com.par9uet.jm.core.common.filterBlockedTags

class HistoryComicPagingSource(
    private val userRepository: UserRepository,
    private val blockedTagList: List<String> = listOf(),
) : PagingSource<Int, Comic>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Comic> {
        val currentPage = params.key ?: 1
        return when (val data =
            userRepository.getHistoryComicList(currentPage)) {
            is NetWorkResult.Error -> {
                LoadResult.Error(Exception(data.message))
            }

            is NetWorkResult.Success<UserHistoryComicListResponse> -> {
                // 用「本页是否装满」判断结束，不要用 total 推总页数：
                // 内置数据源拿不到总条目数，返回的是当前页条数，
                // 据此算出的总页数恒为 1，历史记录会只加载第一页。
                // 判断必须用过滤前的原始条数，否则屏蔽标签一多就会被误判成末页。
                val rawSize = data.data.list.size
                val list = data.data.toComicList().filterBlockedTags(blockedTagList)
                val isLastPage = rawSize < params.loadSize
                LoadResult.Page(
                    data = list,
                    prevKey = if (currentPage == 1) null else currentPage - 1,
                    nextKey = if (isLastPage) null else currentPage + 1
                )
            }
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Comic>): Int? = null
}
