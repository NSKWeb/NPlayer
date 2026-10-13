package com.example.nplayer.data.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MediaItemFormattingTest {

    @Test
    fun `formatDuration shows hours and minutes past an hour`() {
        assertEquals("1h 2m", formatDuration(3_723_000L))
    }

    @Test
    fun `formatDuration shows only minutes under an hour`() {
        assertEquals("5m", formatDuration(307_000L))
    }

    @Test
    fun `formatDuration is null when the duration is unknown`() {
        assertNull(formatDuration(0L))
        assertNull(formatDuration(-1L))
    }

    @Test
    fun `formatSize picks the unit at each boundary`() {
        assertEquals("512 KB", formatSize(512L * 1024))
        assertEquals("2 MB", formatSize(2L * 1024 * 1024))
        assertEquals("3.0 GB", formatSize(3L * (1L shl 30)))
    }
}
