package com.example.nplayer.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SeekStepsTest {

    @Test
    fun `quick jumps include twenty and forty minute hops in both directions`() {
        val labels = QUICK_SEEK_STEPS.map { it.label }
        assertTrue("+20m" in labels)
        assertTrue("+40m" in labels)
        assertTrue("−20m" in labels)
        assertTrue("−40m" in labels)
    }

    @Test
    fun `forward steps are signed positive and backward steps negative`() {
        QUICK_SEEK_STEPS.forEach { step ->
            if (step.label.startsWith("+")) assertTrue(step.deltaMillis > 0)
            if (step.label.startsWith("−")) assertTrue(step.deltaMillis < 0)
            assertEquals(step.deltaMillis > 0, step.isForward)
        }
    }

    @Test
    fun `a jump moves the position by its delta`() {
        assertEquals(10_000L, seekTarget(0L, 10_000L, 3_600_000L))
        assertEquals(20 * 60_000L, seekTarget(0L, 20 * 60_000L, 3_600_000L))
        // 30 minutes back by 20 lands on 10 minutes.
        assertEquals(10 * 60_000L, seekTarget(30 * 60_000L, -20 * 60_000L, 3_600_000L))
    }

    @Test
    fun `a jump never seeks before the start`() {
        assertEquals(0L, seekTarget(5_000L, -20 * 60_000L, 3_600_000L))
    }

    @Test
    fun `a jump never seeks past the end of a known duration`() {
        assertEquals(3_600_000L, seekTarget(3_500_000L, 40 * 60_000L, 3_600_000L))
    }

    @Test
    fun `an unknown duration leaves forward jumps open ended`() {
        assertEquals(20 * 60_000L, seekTarget(0L, 20 * 60_000L, 0L))
    }
}
