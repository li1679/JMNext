package com.par9uet.jm.ui.feature.user

import androidx.paging.PagingSource
import com.par9uet.jm.core.model.Comic
import com.par9uet.jm.data.network.model.NetWorkResult
import com.par9uet.jm.data.network.model.UserHistoryComicListResponse
import com.par9uet.jm.data.repository.ComicTagFilter
import com.par9uet.jm.data.repository.UserRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryComicPagingSourceTest {
    private val repository = mockk<UserRepository>()
    private val tagFilter = mockk<ComicTagFilter>()

    init {
        coEvery { tagFilter.filter(any(), any()) } answers {
            NetWorkResult.Success(firstArg<List<Comic>>())
        }
    }

    @Test
    fun repeatedServerPageStopsAndDoesNotAppendDuplicates() = runTest {
        coEvery { repository.getHistoryComicList(any()) } returns response((1..20).toList())
        val source = source()
        assertEquals(2, source.page(1).nextKey)
        val repeated = source.page(2)
        assertTrue(repeated.data.isEmpty())
        assertNull(repeated.nextKey)
    }

    @Test
    fun overlappingPagesOnlyAppendNewIdsAndEmptyPageEndsLoading() = runTest {
        coEvery { repository.getHistoryComicList(1) } returns response(listOf(1, 1, 2))
        coEvery { repository.getHistoryComicList(2) } returns response(listOf(2, 3))
        coEvery { repository.getHistoryComicList(3) } returns response(emptyList())
        val source = source()
        assertEquals(listOf(1, 2), source.page(1).data.map { it.id })
        val second = source.page(2)
        assertEquals(listOf(3), second.data.map { it.id })
        assertEquals(3, second.nextKey)
        assertNull(source.page(3).nextKey)
    }

    @Test
    fun initialLoadSizeDoesNotDefineServerPageSize() = runTest {
        coEvery { repository.getHistoryComicList(1) } returns response((1..20).toList())
        val page = source().load(PagingSource.LoadParams.Refresh(null, 60, false))
            as PagingSource.LoadResult.Page
        assertEquals(2, page.nextKey)
    }

    @Test
    fun excludedItemsStillAdvancePaginationAndDetectRepeatedPage() = runTest {
        coEvery { repository.getHistoryComicList(any()) } returns response(listOf(1, 2))
        coEvery { tagFilter.filter(any(), any()) } returns NetWorkResult.Success(emptyList())
        val source = source()
        val first = source.page(1)
        assertTrue(first.data.isEmpty())
        assertEquals(2, first.nextKey)
        assertNull(source.page(2).nextKey)
    }

    @Test
    fun refreshAllowsPreviouslySeenItemsInNewOrder() = runTest {
        coEvery { repository.getHistoryComicList(1) } returns response(listOf(1))
        coEvery { repository.getHistoryComicList(2) } returns response(listOf(2))
        val source = source()
        source.page(1)
        source.page(2)
        coEvery { repository.getHistoryComicList(1) } returns response(listOf(2))
        coEvery { repository.getHistoryComicList(2) } returns response(listOf(1))
        assertEquals(listOf(2), source.page(1).data.map { it.id })
        assertEquals(listOf(1), source.page(2).data.map { it.id })
    }

    @Test
    fun failedFilteringCanRetryWithoutLosingItems() = runTest {
        coEvery { repository.getHistoryComicList(1) } returns response(listOf(1))
        coEvery { repository.getHistoryComicList(2) } returns response(listOf(2))
        val source = source()
        source.page(1)
        coEvery { tagFilter.filter(any(), any()) } returns NetWorkResult.Error("failed")
        assertTrue(source.load(PagingSource.LoadParams.Append(2, 20, false)) is PagingSource.LoadResult.Error)
        coEvery { tagFilter.filter(any(), any()) } answers {
            NetWorkResult.Success(firstArg<List<Comic>>())
        }
        assertEquals(listOf(2), source.page(2).data.map { it.id })
    }

    private fun source() = HistoryComicPagingSource(repository, listOf("excluded"), tagFilter)

    private suspend fun HistoryComicPagingSource.page(page: Int): PagingSource.LoadResult.Page<Int, Comic> =
        load(if (page == 1) PagingSource.LoadParams.Refresh(null, 20, false)
        else PagingSource.LoadParams.Append(page, 20, false)) as PagingSource.LoadResult.Page<Int, Comic>

    private fun response(ids: List<Int>): NetWorkResult<UserHistoryComicListResponse> =
        NetWorkResult.Success(UserHistoryComicListResponse(
            list = ids.map { id ->
                UserHistoryComicListResponse.ListItem(
                    id = id.toString(), author = "author", description = "", name = "comic $id",
                    image = "", category = UserHistoryComicListResponse.ListItem.Category(null, null),
                    category_sub = UserHistoryComicListResponse.ListItem.Category(null, null),
                )
            },
            total = ids.size,
        ))
}
