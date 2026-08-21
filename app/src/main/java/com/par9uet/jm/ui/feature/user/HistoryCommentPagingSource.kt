package com.par9uet.jm.ui.feature.user

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.par9uet.jm.core.model.Comment
import com.par9uet.jm.data.repository.UserRepository
import com.par9uet.jm.data.network.model.NetWorkResult
import com.par9uet.jm.data.network.model.UserHistoryCommentListResponse

class HistoryCommentPagingSource(
    private val userRepository: UserRepository,
    private val userId: Int,
) : PagingSource<Int, Comment>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Comment> {
        val currentPage = params.key ?: 1
        return when (val data = userRepository.getHistoryCommentList(currentPage, userId)) {
            is NetWorkResult.Error -> {
                LoadResult.Error(Exception(data.message))
            }

            is NetWorkResult.Success<UserHistoryCommentListResponse> -> {
                val list = data.data.toCommentList()
                // 以本页是否装满判断结束：内置数据源的 total 是当前页条数而非总数
                val isLastPage = data.data.list.size < params.loadSize
                LoadResult.Page(
                    data = list,
                    prevKey = if (currentPage == 1) null else currentPage - 1,
                    nextKey = if (isLastPage) null else currentPage + 1
                )
            }
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Comment>): Int? = null
}