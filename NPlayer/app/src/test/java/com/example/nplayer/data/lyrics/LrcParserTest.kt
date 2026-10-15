package com.example.nplayer.data.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LrcParserTest {

    @Test
    fun `parses timestamped lines in order`() {
        val lyrics = parseLrc(
            """
            [00:12.00]First line
            [00:05.50]Second line
            [01:02.25]Third line
            """.trimIndent(),
        )

        assertEquals(listOf("Second line", "First line", "Third line"), lyrics.lines.map { it.text })
        assertEquals(5_500L, lyrics.lines[0].timeMillis)
        assertEquals(12_000L, lyrics.lines[1].timeMillis)
        assertEquals(62_250L, lyrics.lines[2].timeMillis)
    }

    @Test
    fun `reads title and artist metadata`() {
        val lyrics = parseLrc(
            """
            [ti:Some Song]
            [ar:Some Artist]
            [00:01.00]Hello
            """.trimIndent(),
        )

        assertEquals("Some Song", lyrics.title)
        assertEquals("Some Artist", lyrics.artist)
        assertEquals(1, lyrics.lines.size)
    }

    @Test
    fun `a line with several timestamps becomes several entries`() {
        val lyrics = parseLrc("[00:01.00][00:31.00]Repeated chorus")
        assertEquals(2, lyrics.lines.size)
        assertEquals(listOf("Repeated chorus", "Repeated chorus"), lyrics.lines.map { it.text })
        assertEquals(1_000L, lyrics.lines[0].timeMillis)
        assertEquals(31_000L, lyrics.lines[1].timeMillis)
    }

    @Test
    fun `accepts one two and three digit fractions`() {
        val lyrics = parseLrc(
            """
            [00:01.5]One digit
            [00:02.25]Two digits
            [00:03.125]Three digits
            """.trimIndent(),
        )

        assertEquals(listOf(1_500L, 2_250L, 3_125L), lyrics.lines.map { it.timeMillis })
    }

    @Test
    fun `strips inline word timestamps from enhanced lrc`() {
        val lyrics = parseLrc("[00:01.00]<00:01.00>Hello <00:01.50>world")
        assertEquals("Hello world", lyrics.lines.single().text)
    }

    @Test
    fun `skips blank and unrecognised lines`() {
        val lyrics = parseLrc(
            """
            [by:Someone]

            not a timestamped line
            [00:01.00]Real line
            """.trimIndent(),
        )

        assertEquals(listOf("Real line"), lyrics.lines.map { it.text })
    }

    @Test
    fun `an empty source yields empty lyrics`() {
        assertTrue(parseLrc("").isEmpty)
        assertTrue(parseLrc("[ti:Only metadata]").isEmpty)
    }

    @Test
    fun `active index finds the line playing at a position`() {
        val lines = listOf(
            LyricLine(1_000, "a"),
            LyricLine(5_000, "b"),
            LyricLine(9_000, "c"),
        )

        assertEquals(-1, activeLyricIndex(lines, 0))
        assertEquals(-1, activeLyricIndex(lines, 999))
        assertEquals(0, activeLyricIndex(lines, 1_000))
        assertEquals(0, activeLyricIndex(lines, 4_999))
        assertEquals(1, activeLyricIndex(lines, 5_000))
        assertEquals(2, activeLyricIndex(lines, 100_000))
    }

    @Test
    fun `active index is -1 for no lines`() {
        assertEquals(-1, activeLyricIndex(emptyList(), 5_000))
    }
}
