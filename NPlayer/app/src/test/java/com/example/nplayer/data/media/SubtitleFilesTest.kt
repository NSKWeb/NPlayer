package com.example.nplayer.data.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SubtitleFilesTest {

    @Test
    fun `exact stem matches`() {
        assertTrue(sidecarMatchesMedia("Movie.mkv", "Movie.srt"))
    }

    @Test
    fun `language tag after the stem matches`() {
        assertTrue(sidecarMatchesMedia("Trip.2024.mp4", "Trip.2024.en.srt"))
        assertTrue(sidecarMatchesMedia("Trip.2024.mp4", "Trip.2024.en.vtt"))
    }

    @Test
    fun `an underscore separated tag matches`() {
        assertTrue(sidecarMatchesMedia("Show.mp4", "Show_en.srt"))
    }

    @Test
    fun `case is ignored`() {
        assertTrue(sidecarMatchesMedia("Movie.MKV", "movie.SRT"))
    }

    @Test
    fun `an unrelated name does not match`() {
        assertFalse(sidecarMatchesMedia("Movie.mkv", "AnotherFilm.srt"))
        // A shared prefix is not enough; the separator matters.
        assertFalse(sidecarMatchesMedia("Movie.mkv", "MovieTrailer.srt"))
    }

    @Test
    fun `a partial stem does not match a longer media name`() {
        assertFalse(sidecarMatchesMedia("Trip.2024.mp4", "Trip.srt"))
    }

    @Test
    fun `only supported extensions are accepted`() {
        assertTrue(isSubtitleExtension("srt"))
        assertTrue(isSubtitleExtension("VTT"))
        assertTrue(isSubtitleExtension("ass"))
        assertFalse(isSubtitleExtension("sub"))
        assertFalse(isSubtitleExtension("mp4"))
    }

    @Test
    fun `mime types follow the format`() {
        assertEquals("application/x-subrip", sidecarMimeType("srt"))
        assertEquals("text/vtt", sidecarMimeType("vtt"))
        assertEquals("text/x-ssa", sidecarMimeType("ssa"))
        assertNull(sidecarMimeType("nope"))
    }

    @Test
    fun `a known language tag becomes a name`() {
        assertEquals("English", languageDisplayName("en"))
        assertEquals("English", languageDisplayName("eng"))
        assertEquals("Spanish", languageDisplayName("es"))
        assertNull(languageDisplayName("und"))
        assertNull(languageDisplayName(null))
        assertEquals("XX", languageDisplayName("xx"))
    }

    @Test
    fun `the sidecar language is read from the file name`() {
        assertEquals("English", sidecarLanguage("Trip.2024.en.srt"))
        assertNull(sidecarLanguage("Trip.2024.srt"))
    }

    @Test
    fun `a release tag is not mistaken for a language`() {
        // "2024" and "1080p" sit where a language tag would, but are not languages.
        assertNull(sidecarLanguage("Trip.2024.srt"))
        assertNull(sidecarLanguage("Movie.1080p.srt"))
        assertEquals("Subtitles", sidecarLabel("Trip.2024.mp4", "Trip.2024.srt"))
    }

    @Test
    fun `the language tag is handed to Media3 as a code`() {
        assertEquals("en", sidecarLanguageTag("Trip.2024.en.srt"))
        assertNull(sidecarLanguageTag("Trip.2024.srt"))
    }

    @Test
    fun `the label prefers a language name`() {
        assertEquals("English", sidecarLabel("Trip.2024.mp4", "Trip.2024.en.srt"))
        // Without a language, the extra tag is shown on its own.
        assertEquals("Directors cut", sidecarLabel("Movie.mkv", "Movie.Directors cut.srt"))
    }
}
