package com.par9uet.jm.data.repository

import com.par9uet.jm.core.model.Comic
import com.par9uet.jm.data.network.model.ComicDetailResponse
import com.par9uet.jm.data.network.model.NetWorkResult
import java.lang.reflect.Proxy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ComicTagFilterTest {
    private fun repository(load: (Int) -> NetWorkResult<ComicDetailResponse>) = Proxy.newProxyInstance(
        ComicRepository::class.java.classLoader, arrayOf(ComicRepository::class.java),
    ) { _, method, args ->
        check(method.name == "getComicDetail")
        load(args[0] as Int)
    } as ComicRepository

    private fun detail(id: Int, tags: List<String> = emptyList(), roles: List<String> = emptyList(), works: List<String> = emptyList()) =
        ComicDetailResponse(id, "item", "", emptyList(), 0, 0, 0, tags, roles, works, false, false, emptyList(), "", "0", false)

    @Test fun missingListTagsAreResolvedForSingleRuleAndCachedAcrossRuleChanges() = runBlocking {
        var requests = 0
        val filter = ComicTagFilter(repository { id -> requests++; NetWorkResult.Success(detail(id, listOf("a"))) })
        val items = listOf(Comic.create(1, "item", emptyList()))
        assertTrue((filter.filter(items, listOf(" A ")) as NetWorkResult.Success).data.isEmpty())
        assertEquals(1, (filter.filter(items, listOf("b")) as NetWorkResult.Success).data.size)
        assertEquals(1, requests)
        assertEquals(items, (filter.filter(items, emptyList()) as NetWorkResult.Success).data)
    }

    @Test fun rolesAndWorksAreExcludedAndEmptyTagsAreKnownOnlyAfterDetail() = runBlocking {
        val filter = ComicTagFilter(repository { id -> NetWorkResult.Success(when (id) {
            1 -> detail(id, roles = listOf("blocked"))
            2 -> detail(id, works = listOf("blocked"))
            else -> detail(id)
        }) })
        val items = (1..3).map { Comic.create(it, "item", emptyList()) }
        val visible = (filter.filter(items, listOf("blocked")) as NetWorkResult.Success).data
        assertEquals(listOf(3), visible.map { it.id })
        assertTrue(visible.single().tagsComplete)
    }

    @Test fun failedCheckIsNotAllowedOrCachedAndCanBeRetried() = runBlocking {
        var requests = 0
        val filter = ComicTagFilter(repository { id ->
            if (++requests == 1) NetWorkResult.Error("offline") else NetWorkResult.Success(detail(id, listOf("a")))
        })
        val items = listOf(Comic.create(1, "item", emptyList()))
        assertTrue(filter.filter(items, listOf("a")) is NetWorkResult.Error)
        assertTrue((filter.filter(items, listOf("a")) as NetWorkResult.Success).data.isEmpty())
        assertEquals(2, requests)
    }

    @Test fun cancellationIsPropagated() {
        val filter = ComicTagFilter(repository { throw CancellationException("cancelled") })
        assertThrows(CancellationException::class.java) {
            runBlocking { filter.filter(listOf(Comic.create(1, "item", emptyList())), listOf("a")) }
        }
    }
}
