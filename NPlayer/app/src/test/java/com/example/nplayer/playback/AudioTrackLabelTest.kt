package com.example.nplayer.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioTrackLabelTest {

    @Test
    fun `a container label wins and gains the channel layout`() {
        assertEquals(
            "Commentary · Stereo",
            audioTrackLabel(label = "Commentary", language = "eng", channelCount = 2, index = 0),
        )
    }

    @Test
    fun `the language is used when there is no label`() {
        assertEquals("English · 5.1", audioTrackLabel(null, "eng", 6, 0))
    }

    @Test
    fun `the channel layout alone is used when nothing else is known`() {
        assertEquals("Mono", audioTrackLabel(null, null, 1, 0))
    }

    @Test
    fun `falls back to the track number when nothing is known`() {
        assertEquals("Track 3", audioTrackLabel(null, null, 0, 2))
    }

    @Test
    fun `does not repeat a channel word already in the label`() {
        val label = audioTrackLabel("Stereo mix", "eng", 2, 0)
        assertEquals("Stereo mix", label)
    }

    @Test
    fun `an unknown language code is left as-is`() {
        val label = audioTrackLabel(null, "xyz", 0, 0)
        assertTrue(label.contains("XYZ") || label.contains("xyz"))
    }
}
