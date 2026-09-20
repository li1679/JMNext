package com.par9uet.jm.core.common

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadSpeedTrackerTest {
    @Test
    fun stoppingOneChapterKeepsSiblingSpeed() {
        DownloadSpeedTracker.startTracking(101, 10)
        DownloadSpeedTracker.addBytes(101, 100)
        DownloadSpeedTracker.startTracking(102, 10)
        assertTrue(DownloadSpeedTracker.speedByGroup.value.getValue(10) > 0)
        DownloadSpeedTracker.addBytes(102, 200)
        DownloadSpeedTracker.stopTracking(101)
        assertTrue(DownloadSpeedTracker.speedByGroup.value.getValue(10) > 0)
        DownloadSpeedTracker.stopTracking(102)
        assertFalse(DownloadSpeedTracker.speedByGroup.value.containsKey(10))
    }

    @Test
    fun concurrentGroupsDoNotLoseOtherGroups() {
        val threads = (1..16).map { id ->
            Thread {
                DownloadSpeedTracker.startTracking(1000 + id, 1000 + id)
                repeat(100) { DownloadSpeedTracker.addBytes(1000 + id, 1) }
            }.apply { start() }
        }
        threads.forEach { it.join() }
        (1..16).forEach { id ->
            assertTrue(DownloadSpeedTracker.speedByGroup.value.getValue(1000 + id) > 0)
            DownloadSpeedTracker.stopTracking(1000 + id)
        }
    }
}
