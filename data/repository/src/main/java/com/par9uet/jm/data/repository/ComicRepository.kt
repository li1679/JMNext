package com.par9uet.jm.data.repository

import com.par9uet.jm.core.model.ComicSearchOrderFilter
import com.par9uet.jm.data.network.model.CollectComicResponse
import com.par9uet.jm.data.network.model.ComicDetailResponse
import com.par9uet.jm.data.network.model.ComicListResponse
import com.par9uet.jm.data.network.model.ComicPicListResponse
import com.par9uet.jm.data.network.model.CommentComicResponse
import com.par9uet.jm.data.network.model.CommentListResponse
import com.par9uet.jm.data.network.model.HomeSwiperComicListItemResponse
import com.par9uet.jm.data.network.model.LikeComicResponse
import com.par9uet.jm.data.network.model.NetWorkResult
import com.par9uet.jm.data.network.model.WeekRecommendComicResponse
import com.par9uet.jm.data.network.model.WeekResponse

interface ComicRepository {
    suspend fun getComicDetail(id: Int): NetWorkResult<ComicDetailResponse>
    suspend fun likeComic(id: Int): NetWorkResult<LikeComicResponse>
    suspend fun collectComic(id: Int): NetWorkResult<CollectComicResponse>
    suspend fun unCollectComic(id: Int): NetWorkResult<CollectComicResponse>
    suspend fun getHomeSwiperComicList(): NetWorkResult<List<HomeSwiperComicListItemResponse>>
    suspend fun getComicPicList(id: Int, shunt: String): NetWorkResult<ComicPicListResponse>
    suspend fun downloadImageBytes(comicId: Int, imageIndex: Int): ByteArray?
    suspend fun getComicList(
        page: Int,
        order: ComicSearchOrderFilter,
        searchContent: String,
    ): NetWorkResult<ComicListResponse>

    suspend fun getWeekData(): NetWorkResult<WeekResponse>
    suspend fun getWeekRecommendComicList(
        page: Int,
        categoryId: String,
        typeId: String,
    ): NetWorkResult<WeekRecommendComicResponse>

    suspend fun getCommentList(
        page: Int,
        comicId: Int,
    ): NetWorkResult<CommentListResponse>

    suspend fun comment(
        content: String,
        comicId: Int,
        commentId: Int?
    ): NetWorkResult<CommentComicResponse>

    suspend fun likeComment(commentId: Int): NetWorkResult<CommentComicResponse>

    suspend fun createFavoriteFolder(name: String): NetWorkResult<Unit>
    suspend fun deleteFavoriteFolder(folderId: String): NetWorkResult<Unit>
    suspend fun renameFavoriteFolder(folderId: String, newName: String): NetWorkResult<Unit>
    suspend fun moveComicToFolder(comicId: Int, folderId: String): NetWorkResult<Unit>

}