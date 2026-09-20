package com.par9uet.jm.ui.feature.reader

import android.content.Context
import coil.ImageLoader
import com.par9uet.jm.core.common.ToastManager
import com.par9uet.jm.data.database.dao.DownloadComicDao
import com.par9uet.jm.data.network.model.ComicPicListResponse
import com.par9uet.jm.data.network.model.NetWorkResult
import com.par9uet.jm.data.repository.ComicRepository
import com.par9uet.jm.data.storage.LocalSettingManager
import com.par9uet.jm.domain.store.ReadHistoryManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Test
import io.mockk.mockk
import io.mockk.coEvery

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ComicReadViewModelTest {
    @Test
    fun lateDetailCannotStartImagesAfterLeavingReader() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = mockk<ComicRepository>()
            val release = CompletableDeferred<Unit>()
            coEvery { repository.getComicDetail(1) } coAnswers {
                withContext(NonCancellable) { release.await() }
                NetWorkResult.Error("late error")
            }
            val model = ComicReadViewModel(
                repository, mockk(), mockk(), mockk(), ToastManager(), ReadHistoryManager(mockk(relaxed = true))
            )
            var callbacks = 0
            model.loadChapter(1, false, mockk()) { callbacks++ }
            runCurrent()
            model.leaveReader()
            release.complete(Unit)
            runCurrent()
            assertEquals(0, callbacks)
            assertEquals(-1, model.loadedComicId.intValue)
            assertEquals(false, model.comicDetailState.value.isError)
            io.mockk.coVerify(exactly = 0) { repository.getComicPicList(any()) }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun replacedChapterCannotPublishImagesOrSuccess() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = mockk<ComicRepository>()
            val first = CompletableDeferred<NetWorkResult<ComicPicListResponse>>()
            val second = CompletableDeferred<NetWorkResult<ComicPicListResponse>>()
            coEvery { repository.getComicDetail(any()) } returns NetWorkResult.Error("detail unavailable")
            coEvery { repository.getComicPicList(1) } coAnswers { withContext(NonCancellable) { first.await() } }
            coEvery { repository.getComicPicList(2) } coAnswers { second.await() }
            val model = ComicReadViewModel(
                repository, mockk<ImageLoader>(), mockk<LocalSettingManager>(),
                mockk<DownloadComicDao>(), ToastManager(), ReadHistoryManager(mockk(relaxed = true))
            )
            val callbacks = mutableListOf<Int>()
            val context = mockk<Context>()
            model.loadChapter(1, false, context) { callbacks += 1 }
            runCurrent()
            model.loadChapter(2, false, context) { callbacks += 2 }
            runCurrent()
            second.complete(NetWorkResult.Success(ComicPicListResponse(listOf("second.webp"), 2, 0, "1")))
            runCurrent()
            first.complete(NetWorkResult.Success(ComicPicListResponse(listOf("first.webp"), 1, 0, "1")))
            runCurrent()
            assertEquals(model.comicPicState.value.errorMsg, listOf(2), callbacks)
            assertEquals(2, model.loadedComicId.intValue)
            assertEquals("second.webp", model.comicPicState.value.data!!.single().originSrc)
            model.leaveReader()
        } finally {
            Dispatchers.resetMain()
        }
    }
}
