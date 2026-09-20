package com.par9uet.jm.ui.feature.search

import com.par9uet.jm.ui.feature.search.SearchComicFilter

import com.par9uet.jm.ui.feature.search.SearchComicPagingSource

import androidx.paging.PagingSource
import com.par9uet.jm.core.model.Comic
import com.par9uet.jm.core.model.ComicSearchOrderFilter
import com.par9uet.jm.data.repository.ComicRepository
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
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchComicPagingSourceTest {
    @Test
    fun filtersSingleKnownTagAndKeepsServerPagination() = runTest {
        val repository = FakeComicRepository()
        val source = SearchComicPagingSource(repository, SearchComicFilter(excludedTags = listOf("a")))
        val result = source.load(PagingSource.LoadParams.Refresh(null, 60, false)) as PagingSource.LoadResult.Page
        assertEquals(listOf(2), result.data.map { it.id })
        assertEquals(2, result.nextKey)
    }

    @Test
    fun filtersMultipleKnownTags() = runTest {
        val repository = FakeComicRepository()
        val source = SearchComicPagingSource(
            comicRepository = repository,
            filter = SearchComicFilter(
                searchContent = "artist",
                excludedTags = listOf("a", "b")
            )
        )

        val result = source.load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 20,
                placeholdersEnabled = false
            )
        )

        val page = result as PagingSource.LoadResult.Page<Int, Comic>
        assertEquals("artist -a -b", repository.lastSearchContent)
        assertEquals(listOf(2), page.data.map { it.id })
    }

    private class FakeComicRepository : ComicRepository {
        override suspend fun getCategories(): NetWorkResult<List<com.par9uet.jm.core.model.ComicCategory>> = unused()
        override suspend fun getCategoryComics(page: Int, filter: com.par9uet.jm.core.model.CategoryFilter): NetWorkResult<com.par9uet.jm.core.model.CategoryComicPage> = unused()
        var lastSearchContent: String? = null

        override suspend fun getComicList(
            page: Int,
            order: ComicSearchOrderFilter,
            searchContent: String
        ): NetWorkResult<ComicListResponse> {
            lastSearchContent = searchContent
            return NetWorkResult.Success(
                ComicListResponse(
                    search_query = searchContent,
                    total = "2",
                    redirect_aid = null,
                    nextPage = 2,
                    content = listOf(
                        contentItem(id = 1),
                        contentItem(id = 2)
                    )
                )
            )
        }

        override suspend fun getComicDetail(id: Int): NetWorkResult<ComicDetailResponse> {
            error("List filtering must not request details")
        }

        override suspend fun likeComic(id: Int): NetWorkResult<LikeComicResponse> = unused()

        override suspend fun collectComic(id: Int): NetWorkResult<CollectComicResponse> = unused()

        override suspend fun unCollectComic(id: Int): NetWorkResult<CollectComicResponse> = unused()

        override suspend fun getHomeSwiperComicList(): NetWorkResult<List<HomeSwiperComicListItemResponse>> = unused()

        override suspend fun getComicPicList(id: Int): NetWorkResult<ComicPicListResponse> = unused()

        override suspend fun downloadImageBytes(comicId: Int, imageIndex: Int): ByteArray? = unused()

        override suspend fun getWeekData(): NetWorkResult<WeekResponse> = unused()

        override suspend fun getWeekRecommendComicList(
            page: Int,
            categoryId: String,
            typeId: String
        ): NetWorkResult<WeekRecommendComicResponse> = unused()

        override suspend fun getCommentList(page: Int, comicId: Int): NetWorkResult<CommentListResponse> = unused()

        override suspend fun comment(
            content: String,
            comicId: Int,
            commentId: Int?
        ): NetWorkResult<CommentComicResponse> = unused()

        override suspend fun likeComment(commentId: Int): NetWorkResult<CommentComicResponse> = unused()

        override suspend fun createFavoriteFolder(name: String): NetWorkResult<Unit> = unused()

        override suspend fun deleteFavoriteFolder(folderId: String): NetWorkResult<Unit> = unused()

        override suspend fun renameFavoriteFolder(folderId: String, newName: String): NetWorkResult<Unit> = unused()

        override suspend fun moveComicToFolder(comicId: Int, folderId: String): NetWorkResult<Unit> = unused()

        private fun contentItem(id: Int): ComicListResponse.ContentListItem {
            val category = ComicListResponse.ContentListItem.Category(id = null, title = "category")
            return ComicListResponse.ContentListItem(
                id = id.toString(),
                author = "author",
                description = "",
                name = "comic $id",
                image = "",
                category = category,
                category_sub = category,
                liked = false,
                is_favorite = false,
                update_at = 0,
                tags = if (id == 1) listOf("a") else null
            )
        }

        private fun unused(): Nothing {
            throw UnsupportedOperationException("Unused fake repository method")
        }
    }
}
