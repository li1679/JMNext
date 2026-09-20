package com.par9uet.jm.ui.feature.home

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TabDoubleTapTrackerTest {
    @Test
    fun onlyTwoQuickClicksOnTheSameSupportedTabTrigger() {
        for (route in listOf("home", "category", "collect")) {
            val tracker = TabDoubleTapTracker(300)
            assertFalse(tracker.click(route, 1000))
            assertTrue(tracker.click(route, 1200))
            assertFalse(tracker.click(route, 1300))
        }
    }

    @Test
    fun slowClicksTabSwitchesAndUserTabDoNotTrigger() {
        val tracker = TabDoubleTapTracker(300)
        assertFalse(tracker.click("home", 1000))
        assertFalse(tracker.click("home", 1400))
        assertFalse(tracker.click("category", 1500))
        assertFalse(tracker.click("collect", 1600))
        assertFalse(tracker.click("user", 1700))
        assertFalse(tracker.click("user", 1800))
    }
}
