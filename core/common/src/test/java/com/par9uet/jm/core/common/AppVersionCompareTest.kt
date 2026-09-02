package com.par9uet.jm.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppVersionCompareTest {
    @Test
    fun normalizesReleaseTagsBeforeComparing() {
        assertTrue(compareVersion("v1.2.0", "1.1.9") > 0)
        assertEquals(0, compareVersion("1.1.0-beta", "v1.1.0"))
    }

    @Test
    fun treatsMissingPatchPartsAsZero() {
        assertEquals(0, compareVersion("1.2", "1.2.0"))
        assertTrue(compareVersion("1.10.0", "1.9.9") > 0)
    }
}
