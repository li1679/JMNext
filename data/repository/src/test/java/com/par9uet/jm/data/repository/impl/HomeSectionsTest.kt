package com.par9uet.jm.data.repository.impl

import com.par9uet.jm.data.network.model.HomeSwiperComicListItemResponse
import com.par9uet.jm.data.network.model.NetWorkResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class HomeSectionsTest {
    @Test fun allFailuresAreNotAnEmptySuccess() = runBlocking {
        assertTrue(loadHomeSections(listOf(HomeSectionRequest("a", "A") { error("offline") })) is NetWorkResult.Error)
    }

    @Test fun partialFailureRetainsCategoryAndError() = runBlocking {
        val result = loadHomeSections(listOf(
            HomeSectionRequest("a", "A") { error("offline") },
            HomeSectionRequest("b", "B") { listOf(HomeSwiperComicListItemResponse("b", "B", "b", "builtin", "", emptyList())) },
        )) as NetWorkResult.Success
        assertEquals(listOf("a", "b"), result.data.map { it.id })
        assertTrue(result.data.first().errorMessage!!.contains("offline"))
        assertNull(result.data.last().errorMessage)
    }

    @Test fun cancellationIsNotReportedAsCategoryFailure() {
        assertThrows(CancellationException::class.java) {
            runBlocking { loadHomeSections(listOf(HomeSectionRequest("a", "A") { throw CancellationException() })) }
        }
    }
}
