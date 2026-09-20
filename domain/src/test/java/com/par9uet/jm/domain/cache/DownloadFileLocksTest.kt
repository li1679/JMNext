package com.par9uet.jm.domain.cache

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadFileLocksTest {
    @Test
    fun cancellationBarrierWaitsForOutstandingWriter() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var writerStopped = false
        var deleted = false
        val worker = launch(Dispatchers.Default) {
            DownloadFileLocks.chapter(40).withLock {
                withContext(NonCancellable) {
                    entered.complete(Unit)
                    release.await()
                    writerStopped = true
                }
            }
        }
        entered.await()
        worker.cancel()
        val deletion = launch(Dispatchers.Default) {
            DownloadFileLocks.chapter(40).withLock {
                assertTrue(writerStopped)
                deleted = true
            }
        }
        assertFalse(deleted)
        release.complete(Unit)
        worker.cancelAndJoin()
        deletion.join()
        assertTrue(deleted)
    }

    @Test
    fun cancelledSiblingDoesNotOwnSharedGroupLock() = runBlocking {
        val lock = DownloadFileLocks.group(41)
        lock.lock()
        val sibling = launch { lock.withLock { error("Cancelled sibling entered shared writer") } }
        sibling.cancelAndJoin()
        lock.unlock()
        lock.withLock { assertTrue(lock.isLocked) }
    }
}
