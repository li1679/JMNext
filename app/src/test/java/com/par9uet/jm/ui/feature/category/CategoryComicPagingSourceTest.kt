package com.par9uet.jm.ui.feature.category

import androidx.paging.PagingSource
import com.par9uet.jm.core.model.CategoryComicPage
import com.par9uet.jm.core.model.CategoryFilter
import com.par9uet.jm.core.model.CategoryOrder
import com.par9uet.jm.core.model.Comic
import com.par9uet.jm.data.network.model.NetWorkResult
import com.par9uet.jm.data.repository.ComicRepository
import java.lang.reflect.Proxy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryComicPagingSourceTest {
    @Test
    fun taggedComicsRemainVisibleAndUseServerNextPage() = runTest {
        val filter = CategoryFilter("doujin", "chinese", CategoryOrder.MOST_VIEWED)
        val blockedComic = Comic.create(1, "Blocked item", emptyList()).copy(tagList = listOf("blocked"), tagsComplete = true)
        val source = CategoryComicPagingSource(repository { page, query ->
            assertEquals(2, page)
            assertEquals(filter, query)
            NetWorkResult.Success(CategoryComicPage(listOf(blockedComic), 3))
        }, filter)
        val result = source.load(PagingSource.LoadParams.Append(2, 60, false)) as PagingSource.LoadResult.Page
        assertEquals(3, result.nextKey)
        assertEquals(1, result.prevKey)
        assertEquals(listOf(blockedComic), result.data)
    }

    @Test
    fun subcategoryKeepsParentAndCaseInApiSlug() {
        assertEquals("doujin_CG", CategoryFilter("doujin", "CG").effectiveSlug)
        assertEquals("single_japanese", CategoryFilter("single", "japanese").effectiveSlug)
        assertEquals("doujin", CategoryFilter("doujin").effectiveSlug)
    }

    @Test
    fun terminalPageStopsPaging() = runTest {
        val source = CategoryComicPagingSource(repository { _, _ ->
            NetWorkResult.Success(CategoryComicPage(emptyList(), null))
        }, CategoryFilter())
        val result = source.load(PagingSource.LoadParams.Refresh(null, 20, false)) as PagingSource.LoadResult.Page
        assertNull(result.prevKey)
        assertNull(result.nextKey)
    }

    @Test
    fun repositoryErrorIsVisibleAndRetryRepeatsThePage() = runTest {
        var attempts = 0
        val source = CategoryComicPagingSource(repository { page, _ ->
            assertEquals(3, page)
            attempts++
            if (attempts == 1) NetWorkResult.Error("network unavailable")
            else NetWorkResult.Success(CategoryComicPage(emptyList(), null))
        }, CategoryFilter())
        val params = PagingSource.LoadParams.Append(3, 20, false)
        val error = source.load(params) as PagingSource.LoadResult.Error
        assertEquals("network unavailable", error.throwable.message)
        assertTrue(source.load(params) is PagingSource.LoadResult.Page)
        assertEquals(2, attempts)
    }

    private fun repository(load: (Int, CategoryFilter) -> NetWorkResult<CategoryComicPage>): ComicRepository =
        Proxy.newProxyInstance(ComicRepository::class.java.classLoader, arrayOf(ComicRepository::class.java)) { _, method, args ->
            check(method.name == "getCategoryComics") { "Unexpected call: ${method.name}" }
            load(args[0] as Int, args[1] as CategoryFilter)
        } as ComicRepository
}
