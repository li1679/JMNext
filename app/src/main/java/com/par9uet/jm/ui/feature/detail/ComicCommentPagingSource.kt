package com.par9uet.jm.ui.feature.detail

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.par9uet.jm.core.model.Comment
import com.par9uet.jm.data.repository.ComicRepository
import com.par9uet.jm.data.network.model.CommentListResponse
import com.par9uet.jm.data.network.model.NetWorkResult

class ComicCommentPagingSource(
    private val comicRepository: ComicRepository,
    private val comicId: Int,
) : PagingSource<Int, Comment>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Comment> {
        val currentPage = params.key ?: 1
        return when (val data =
            comicRepository.getCommentList(currentPage, comicId)) {
            is NetWorkResult.Error -> {
                LoadResult.Error(Exception(data.message))
            }

            is NetWorkResult.Success<CommentListResponse> -> {
                val list = data.data.toCommentList()
                val total = data.data.total.toInt()
                val isLastPage = currentPage >= (total + params.loadSize - 1) / params.loadSize
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
