package com.example.nplayer.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerSettingsTest {

    @Test
    fun `speed steps up through the presets`() {
        assertEquals(1.25f, nextSpeed(1f), 0.001f)
        assertEquals(1.5f, nextSpeed(1.25f), 0.001f)
        assertEquals(4f, nextSpeed(3f), 0.001f)
    }

    @Test
    fun `speed stops at the top of the range`() {
        assertEquals(4f, nextSpeed(4f), 0.001f)
        assertEquals(4f, nextSpeed(10f), 0.001f)
    }

    @Test
    fun `speed steps down and stops at the bottom`() {
        assertEquals(0.75f, previousSpeed(1f), 0.001f)
        assertEquals(0.25f, previousSpeed(0.5f), 0.001f)
        assertEquals(0.25f, previousSpeed(0.25f), 0.001f)
    }

    @Test
    fun `a value between presets snaps to the next one up`() {
        assertEquals(1.5f, nextSpeed(1.3f), 0.001f)
        assertEquals(1f, previousSpeed(1.1f), 0.001f)
    }

    @Test
    fun `speed renders without a trailing decimal`() {
        assertEquals("1x", formatSpeed(1f))
        assertEquals("1.5x", formatSpeed(1.5f))
        assertEquals("0.25x", formatSpeed(0.25f))
    }

    @Test
    fun `clock shows minutes under an hour and hours past it`() {
        assertEquals("0:00", formatClock(0))
        assertEquals("0:05", formatClock(5_000))
        assertEquals("1:01", formatClock(61_000))
        assertEquals("59:59", formatClock(3_599_000))
        assertEquals("1:00:00", formatClock(3_600_000))
        assertEquals("2:03:04", formatClock(7_384_000))
    }

    @Test
    fun `clock never goes negative`() {
        assertEquals("0:00", formatClock(-5_000))
    }

    @Test
    fun `sleep remaining reuses the clock format`() {
        assertEquals("45:00", formatSleepRemaining(45 * 60_000L))
        assertEquals("1:05:00", formatSleepRemaining(65 * 60_000L))
        assertEquals("0:00", formatSleepRemaining(-1))
    }

    @Test
    fun `repeat mode cycles off all one`() {
        assertEquals(RepeatMode.ALL, RepeatMode.OFF.next())
        assertEquals(RepeatMode.ONE, RepeatMode.ALL.next())
        assertEquals(RepeatMode.OFF, RepeatMode.ONE.next())
    }

    @Test
    fun `resize mode cycles and wraps`() {
        assertEquals(ResizeMode.FILL, ResizeMode.FIT.next())
        assertEquals(ResizeMode.ZOOM, ResizeMode.FILL.next())
        assertEquals(ResizeMode.STRETCH, ResizeMode.ZOOM.next())
        assertEquals(ResizeMode.FIT, ResizeMode.STRETCH.next())
    }

    @Test
    fun `ab repeat is active only when both ends are set and ordered`() {
        assertFalse(AbRepeat().isActive)
        assertFalse(AbRepeat(startMillis = 1_000).isActive)
        assertFalse(AbRepeat(startMillis = 5_000, endMillis = 5_000).isActive)
        assertFalse(AbRepeat(startMillis = 5_000, endMillis = 1_000).isActive)
        assertTrue(AbRepeat(startMillis = 1_000, endMillis = 5_000).isActive)
    }
}
