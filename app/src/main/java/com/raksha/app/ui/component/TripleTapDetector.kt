package com.raksha.app.ui.component

import java.util.ArrayDeque

class TripleTapDetector(
    private val requiredTaps: Int = 3,
    private val windowMillis: Long = 1_200L
) {
    private val taps = ArrayDeque<Long>()

    fun registerTap(timestampMillis: Long): Boolean {
        while (taps.isNotEmpty() && timestampMillis - taps.first() > windowMillis) {
            taps.removeFirst()
        }
        taps.addLast(timestampMillis)
        if (taps.size >= requiredTaps) {
            taps.clear()
            return true
        }
        return false
    }

    fun reset() {
        taps.clear()
    }
}
