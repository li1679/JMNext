package com.par9uet.jm.data.repository

import com.par9uet.jm.core.model.Comic
import com.par9uet.jm.data.network.model.NetWorkResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ComicTagFilterTest {
    private val filter = ComicTagFilter()
    private fun comic(id: Int) = Comic.create(id, "item", emptyList())

    @Test fun missingTagsRemainVisibleWithoutDetailRequests() = runBlocking {
        val items = (1..60).map(::comic)
        assertEquals(items, (filter.filter(items, listOf("blocked")) as NetWorkResult.Success).data)
    }

    @Test fun knownTagsRolesAndWorksAreExcludedEvenWhenMetadataIsIncomplete() = runBlocking {
        val items = listOf(
            comic(1).copy(tagList = listOf(" BLOCKED ")),
            comic(2).copy(roleList = listOf("blocked")),
            comic(3).copy(workList = listOf("Blocked")),
            comic(4).copy(tagList = listOf("other")),
            comic(5),
            comic(6).copy(tagList = listOf("blocked sequel")),
        )
        val result = filter.filter(items, listOf(" blocked ")) as NetWorkResult.Success
        assertEquals(listOf(4, 5, 6), result.data.map { it.id })
    }

    @Test fun changingOrRemovingRulesImmediatelyRestoresItems() = runBlocking {
        val items = listOf(comic(1).copy(tagList = listOf("a")), comic(2).copy(tagList = listOf("b")))
        assertEquals(listOf(2), (filter.filter(items, listOf("a")) as NetWorkResult.Success).data.map { it.id })
        assertEquals(listOf(1), (filter.filter(items, listOf("b")) as NetWorkResult.Success).data.map { it.id })
        assertEquals(items, (filter.filter(items, listOf(" ")) as NetWorkResult.Success).data)
    }

    @Test fun allKnownMatchesReturnAnEmptySuccessfulPage() = runBlocking {
        val result = filter.filter(listOf(comic(1).copy(tagList = listOf("a"))), listOf("a"))
        assertTrue((result as NetWorkResult.Success).data.isEmpty())
    }
}
