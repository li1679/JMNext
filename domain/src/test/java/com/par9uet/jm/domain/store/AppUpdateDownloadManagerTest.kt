package com.par9uet.jm.domain.store

import android.content.Context
import com.par9uet.jm.core.common.ToastManager
import com.par9uet.jm.domain.notification.cancelProgressNotification
import com.par9uet.jm.domain.notification.showUpdateDownloadedNotification
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.Executors
import java.util.concurrent.TimeoutException
import org.junit.Assert.assertThrows

class AppUpdateDownloadManagerTest {
    @get:Rule val folder = TemporaryFolder()

    @Test
    fun cancelCannotInterleaveWithCompletionNotification() = runBlocking {
        val notifying = CountDownLatch(1)
        val releaseNotification = CountDownLatch(1)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val executor = Executors.newSingleThreadExecutor()
        val context = mockk<Context>()
        every { context.cacheDir } returns folder.root
        mockkStatic("com.par9uet.jm.domain.notification.AppNotificationsKt")
        every { cancelProgressNotification(any(), any()) } returns Unit
        every { showUpdateDownloadedNotification(any(), any(), any()) } answers {
            notifying.countDown()
            check(releaseNotification.await(5, TimeUnit.SECONDS))
        }
        try {
            val client = OkHttpClient.Builder().addInterceptor { chain ->
                Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                    .code(200).message("OK")
                    .body(byteArrayOf(0x50, 0x4b, 0x03, 0x04).toResponseBody()).build()
            }.build()
            val manager = AppUpdateDownloadManager(context, scope, ToastManager(), client)
            manager.start(AppUpdateDownloadRequest("v1", "app.apk", "https://example.test/apk"))
            check(notifying.await(5, TimeUnit.SECONDS))
            val cancelStarted = CountDownLatch(1)
            val canceled = executor.submit {
                cancelStarted.countDown()
                manager.cancel()
            }
            check(cancelStarted.await(5, TimeUnit.SECONDS))
            assertThrows(TimeoutException::class.java) { canceled.get(100, TimeUnit.MILLISECONDS) }
            releaseNotification.countDown()
            canceled.get(5, TimeUnit.SECONDS)
            assertEquals(AppUpdateDownloadStatus.Canceled, manager.state.value.status)
        } finally {
            releaseNotification.countDown()
            scope.cancel()
            executor.shutdownNow()
            unmockkStatic("com.par9uet.jm.domain.notification.AppNotificationsKt")
        }
    }

    @Test
    fun replacedDownloadCannotOverwriteNewFileOrCompleteNotification() = runBlocking {
        val oldEntered = CountDownLatch(1)
        val releaseOld = CountDownLatch(1)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val context = mockk<Context>()
        every { context.cacheDir } returns folder.root
        mockkStatic("com.par9uet.jm.domain.notification.AppNotificationsKt")
        every { cancelProgressNotification(any(), any()) } returns Unit
        every { showUpdateDownloadedNotification(any(), any(), any()) } returns Unit
        try {
            val client = OkHttpClient.Builder().addInterceptor { chain ->
                val old = chain.request().url.encodedPath == "/old"
                if (old) {
                    oldEntered.countDown()
                    check(releaseOld.await(5, TimeUnit.SECONDS))
                }
                Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                    .code(200).message("OK")
                    .body(byteArrayOf(0x50, 0x4b, 0x03, 0x04, if (old) 1 else 2).toResponseBody())
                    .build()
            }.build()
            val manager = AppUpdateDownloadManager(context, scope, ToastManager(), client)
            manager.start(AppUpdateDownloadRequest("old", "app.apk", "https://example.test/old"))
            check(oldEntered.await(5, TimeUnit.SECONDS))
            manager.start(AppUpdateDownloadRequest("new", "app.apk", "https://example.test/new"))
            withTimeout(5_000) {
                while (manager.state.value.status != AppUpdateDownloadStatus.Completed) delay(10)
            }
            releaseOld.countDown()
            delay(100)
            assertEquals("new", manager.state.value.version)
            assertEquals(2, File(manager.state.value.savedPath).readBytes().last().toInt())
            assertFalse(File(folder.root, "common/updates").listFiles().orEmpty().any { it.extension == "part" })
            verify(exactly = 1) { showUpdateDownloadedNotification(context, "new", any()) }
            verify(exactly = 0) { showUpdateDownloadedNotification(context, "old", any()) }
        } finally {
            releaseOld.countDown()
            scope.cancel()
            unmockkStatic("com.par9uet.jm.domain.notification.AppNotificationsKt")
        }
    }
}
