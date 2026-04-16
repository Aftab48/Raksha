package com.raksha.app.ui.component

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TripleTapDetectorTest {

    @Test
    fun `three taps inside window triggers triple tap`() {
        val detector = TripleTapDetector(requiredTaps = 3, windowMillis = 1_200L)

        assertFalse(detector.registerTap(0L))
        assertFalse(detector.registerTap(300L))
        assertTrue(detector.registerTap(900L))
    }

    @Test
    fun `taps outside window do not trigger triple tap`() {
        val detector = TripleTapDetector(requiredTaps = 3, windowMillis = 1_200L)

        assertFalse(detector.registerTap(0L))
        assertFalse(detector.registerTap(700L))
        assertFalse(detector.registerTap(2_100L))
    }
}
