package com.example.nplayer.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SubtitleTracksTest {

    @Test
    fun `tracks list sidecars before embedded and skip duplicates`() {
        val embedded = listOf(
            SubtitleTrack(id = "grp1", label = "English", embedded = true),
        )
        val tracks = buildSubtitleTracks(
            mediaName = "Trip.2024.mp4",
            sidecarNames = listOf("Trip.2024.en.srt", "Trip.2024.srt", "Unrelated.srt"),
            embedded = embedded,
        )

        assertEquals(listOf("Trip.2024.en.srt", "Trip.2024.srt", "grp1"), tracks.map { it.id })
        assertEquals("English", tracks.first().label)
        assertTrue(tracks.first().isExternal)
        assertTrue(tracks.last().embedded)
    }

    @Test
    fun `an unsupported extension is left out of the list`() {
        val tracks = buildSubtitleTracks(
            mediaName = "Movie.mkv",
            sidecarNames = listOf("Movie.sub", "Movie.srt"),
            embedded = emptyList(),
        )
        assertEquals(listOf("Movie.srt"), tracks.map { it.id })
    }

    @Test
    fun `a duplicated sidecar name is listed once`() {
        val tracks = buildSubtitleTracks(
            mediaName = "Movie.mkv",
            sidecarNames = listOf("Movie.srt", "Movie.SRT"),
            embedded = emptyList(),
        )
        assertEquals(1, tracks.size)
    }
}
