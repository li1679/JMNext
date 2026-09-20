package com.par9uet.jm.domain.worker

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class DownloadTimeoutTest {
    @Test
    fun localTimeoutIsRetryableFailure() = runBlocking {
        try {
            withDownloadTimeout(10) { awaitCancellation() }
            fail("Expected timeout")
        } catch (expected: IOException) {
            assertTrue(expected.message!!.contains("超时"))
        }
    }

    @Test
    fun externalCancellationRemainsCancellation() = runBlocking {
        val started = CompletableDeferred<Unit>()
        var cancelled = false
        val job = launch {
            try {
                withDownloadTimeout(60_000) {
                    started.complete(Unit)
                    awaitCancellation()
                }
            } catch (expected: CancellationException) {
                cancelled = true
                throw expected
            }
        }
        started.await()
        job.cancelAndJoin()
        assertTrue(cancelled)
    }
}
