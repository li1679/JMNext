package com.par9uet.jm.domain.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfExportTest {
    @Test
    fun samplingKeepsOriginalLongEdgeWithinLimit() {
        assertEquals(1, calculateSampleSize(1000, 2000))
        assertEquals(4, calculateSampleSize(1000, 8000))
        assertEquals(8, calculateSampleSize(16000, 1000))
        for (size in listOf(2001, 4001, 8001, 16001, Int.MAX_VALUE)) {
            val sample = calculateSampleSize(size, 1000)
            assertTrue((size.toLong() + sample - 1) / sample <= 2000)
        }
    }
}
