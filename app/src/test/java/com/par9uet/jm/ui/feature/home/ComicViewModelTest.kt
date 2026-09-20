package com.par9uet.jm.ui.feature.home

import com.par9uet.jm.data.network.model.HomeSwiperComicListItemResponse
import com.par9uet.jm.data.network.model.NetWorkResult
import com.par9uet.jm.data.repository.ComicRepository
import com.par9uet.jm.data.repository.ComicTagFilter
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ComicViewModelTest {
    @Test fun repeatedRefreshIsCoalescedAndFailureKeepsExistingContent() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = mockk<ComicRepository>()
            val first = CompletableDeferred<NetWorkResult<List<HomeSwiperComicListItemResponse>>>()
            coEvery { repository.getHomeSwiperComicList() } coAnswers { first.await() }
            val model = ComicViewModel(repository, ComicTagFilter())
            model.ensureHomeComic()
            model.refreshHomeComic()
            runCurrent()
            model.refreshHomeComic()
            first.complete(NetWorkResult.Success(listOf(HomeSwiperComicListItemResponse("a", "A", "a", "", "", emptyList()))))
            runCurrent()
            coVerify(exactly = 1) { repository.getHomeSwiperComicList() }
            coEvery { repository.getHomeSwiperComicList() } returns NetWorkResult.Error("offline")
            model.refreshHomeComic()
            runCurrent()
            assertEquals(listOf("a"), model.homeComicState.value.list.map { it.id })
            assertTrue(model.homeComicState.value.isError)
            assertFalse(model.homeComicState.value.isLoading)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
