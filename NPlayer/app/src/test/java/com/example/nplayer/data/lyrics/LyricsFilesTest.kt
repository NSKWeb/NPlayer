package com.example.nplayer.data.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsFilesTest {

    @Test
    fun `matches a sidecar sharing the stem`() {
        assertTrue(lyricsMatchesMedia("Song.mp3", "Song.lrc"))
        assertTrue(lyricsMatchesMedia("Song.flac", "Song.en.lrc"))
        assertTrue(lyricsMatchesMedia("Song.mp3", "Song_live.lrc"))
    }

    @Test
    fun `rejects an unrelated name and a non-lrc extension`() {
        assertFalse(lyricsMatchesMedia("Song.mp3", "Other.lrc"))
        assertFalse(lyricsMatchesMedia("Song.mp3", "Song.txt"))
        assertFalse(lyricsMatchesMedia("Song.mp3", "Song.srt"))
    }

    @Test
    fun `only lrc counts as a lyrics extension`() {
        assertTrue(isLyricsExtension("lrc"))
        assertTrue(isLyricsExtension("LRC"))
        assertFalse(isLyricsExtension("txt"))
        assertFalse(isLyricsExtension("srt"))
    }

    @Test
    fun `picks the exact stem over a tagged one`() {
        val picked = pickLyricsFile("Song.mp3", listOf("Song.en.lrc", "Song.lrc"))
        assertEquals("Song.lrc", picked)
    }

    @Test
    fun `picks nothing when no candidate matches`() {
        assertNull(pickLyricsFile("Song.mp3", listOf("Other.lrc", "Song.txt")))
    }

    @Test
    fun `breaks ties by name so the choice is stable`() {
        val picked = pickLyricsFile("Song.mp3", listOf("Song.zh.lrc", "Song.en.lrc"))
        assertEquals("Song.en.lrc", picked)
    }
}
