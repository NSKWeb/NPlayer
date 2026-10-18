package com.example.nplayer.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The clamping and drag maths behind the ±10 s buttons and the slide-to-seek gesture. */
class SeekTest {

    @Test
    fun `forward step adds the step to the current position`() {
        assertEquals(20_000L, seekTarget(currentPositionMillis = 10_000L, deltaMillis = 10_000L, durationMillis = 60_000L))
    }

    @Test
    fun `backward step subtracts the step`() {
        assertEquals(5_000L, seekTarget(currentPositionMillis = 15_000L, deltaMillis = -10_000L, durationMillis = 60_000L))
    }

    @Test
    fun `backward step never goes before the start`() {
        assertEquals(0L, seekTarget(currentPositionMillis = 4_000L, deltaMillis = -10_000L, durationMillis = 60_000L))
    }

    @Test
    fun `forward step never goes past the end`() {
        assertEquals(60_000L, seekTarget(currentPositionMillis = 55_000L, deltaMillis = 10_000L, durationMillis = 60_000L))
    }

    /**
     * The regression behind the broken ±10 s buttons: with the duration still unset,
     * clamping to it collapsed every target to 0 and the seek went to the start.
     */
    @Test
    fun `unknown duration does not clip a forward step to the start`() {
        assertEquals(15_000L, seekTarget(currentPositionMillis = 5_000L, deltaMillis = 10_000L, durationMillis = 0L))
    }

    @Test
    fun `unknown duration still clamps a backward step to the start`() {
        assertEquals(0L, seekTarget(currentPositionMillis = 1_000L, deltaMillis = -10_000L, durationMillis = 0L))
    }

    @Test
    fun `dragging the full width covers the whole file`() {
        assertEquals(60_000L, dragSeekDelta(fraction = 1f, durationMillis = 60_000L))
    }

    @Test
    fun `dragging a fraction of the width covers that fraction of the file`() {
        assertEquals(15_000L, dragSeekDelta(fraction = 0.25f, durationMillis = 60_000L))
    }

    @Test
    fun `dragging backwards gives a negative delta`() {
        assertEquals(-30_000L, dragSeekDelta(fraction = -0.5f, durationMillis = 60_000L))
    }

    /**
     * The other half of the slide-to-seek bug: scaling by an unknown duration gave a
     * delta of 0, so the drag covered nothing. The swipe now falls back to a fixed span.
     */
    @Test
    fun `unknown duration still moves the swipe by the fallback span`() {
        assertEquals(
            (DRAG_SEEK_FALLBACK_MILLIS / 2),
            dragSeekDelta(fraction = 0.5f, durationMillis = 0L),
        )
    }

    @Test
    fun `a full swipe covers the fallback span when the duration is unknown`() {
        assertEquals(
            DRAG_SEEK_FALLBACK_MILLIS,
            dragSeekDelta(fraction = 1f, durationMillis = 0L),
        )
    }

    @Test
    fun `a swipe never yields zero even with an unknown duration`() {
        assertTrue(dragSeekDelta(fraction = 0.05f, durationMillis = 0L) > 0)
    }
}
