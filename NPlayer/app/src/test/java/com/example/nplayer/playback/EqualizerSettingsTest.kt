package com.example.nplayer.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EqualizerSettingsTest {

    @Test
    fun `levels are clamped to the device range`() {
        assertEquals(1500, clampLevel(5000, -1500, 1500))
        assertEquals(-1500, clampLevel(-5000, -1500, 1500))
        assertEquals(300, clampLevel(300, -1500, 1500))
    }

    @Test
    fun `a fraction maps onto the level range`() {
        assertEquals(-1500, levelFromFraction(0f, -1500, 1500))
        assertEquals(0, levelFromFraction(0.5f, -1500, 1500))
        assertEquals(1500, levelFromFraction(1f, -1500, 1500))
    }

    @Test
    fun `a level maps back onto a fraction`() {
        assertEquals(0f, fractionFromLevel(-1500, -1500, 1500))
        assertEquals(0.5f, fractionFromLevel(0, -1500, 1500))
        assertEquals(1f, fractionFromLevel(1500, -1500, 1500))
    }

    @Test
    fun `fraction and level round trip within a millibel`() {
        val level = 750
        val back = levelFromFraction(fractionFromLevel(level, -1500, 1500), -1500, 1500)
        assertEquals(level, back)
    }

    @Test
    fun `a degenerate range does not divide by zero`() {
        assertEquals(0.5f, fractionFromLevel(0, 0, 0))
    }

    @Test
    fun `band frequencies read in hertz and kilohertz`() {
        assertEquals("60 Hz", formatBandFrequency(60))
        assertEquals("1 kHz", formatBandFrequency(1000))
        assertEquals("16 kHz", formatBandFrequency(16000))
        assertEquals("1.5 kHz", formatBandFrequency(1500))
    }

    @Test
    fun `preset matching needs the same shape and values`() {
        assertTrue(matchesPreset(listOf(0, 300, 0), listOf(0, 300, 0)))
        assertFalse(matchesPreset(listOf(0, 300, 0), listOf(0, 300, 0, 0)))
        assertFalse(matchesPreset(listOf(0, 400, 0), listOf(0, 300, 0)))
    }

    @Test
    fun `preset matching tolerates a small drift`() {
        assertTrue(matchesPreset(listOf(0, 310, 0), listOf(0, 300, 0), toleranceMb = 50))
    }
}
