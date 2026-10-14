package com.example.nplayer.data.playlists

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PlaylistCodecTest {

    @Test
    fun `item keys survive an encode and decode round trip`() {
        val keys = listOf("content://media/external/video/media/1", "content://media/external/audio/media/2")
        assertEquals(keys, decodeItemKeys(encodeItemKeys(keys)))
    }

    @Test
    fun `an empty or missing list decodes to nothing`() {
        assertEquals(emptyList<String>(), decodeItemKeys(null))
        assertEquals(emptyList<String>(), decodeItemKeys(""))
    }

    @Test
    fun `the first free id is chosen`() {
        assertEquals("pl_1", nextPlaylistId(emptySet()))
        assertEquals("pl_2", nextPlaylistId(setOf("pl_1")))
        assertEquals("pl_3", nextPlaylistId(setOf("pl_1", "pl_2")))
    }

    @Test
    fun `a repeated name gets a numeric suffix`() {
        assertEquals("Chill", uniquePlaylistName("Chill", emptySet()))
        assertEquals("Chill (2)", uniquePlaylistName("Chill", setOf("Chill")))
        assertEquals("Chill (3)", uniquePlaylistName("Chill", setOf("Chill", "Chill (2)")))
    }

    @Test
    fun `a blank name becomes the default`() {
        assertEquals("Playlist", uniquePlaylistName("   ", emptySet()))
    }

    @Test
    fun `adding ignores a repeat and removing clears every copy`() {
        val keys = addKey(listOf("a"), "b")
        assertEquals(listOf("a", "b"), keys)
        assertEquals(listOf("a", "b"), addKey(keys, "b"))
        assertEquals(listOf("a"), removeKey(listOf("a", "b", "b"), "b"))
    }

    @Test
    fun `ids and names are independent of each other`() {
        assertNotEquals(nextPlaylistId(setOf("pl_1")), uniquePlaylistName("pl_1", setOf("pl_1")))
    }
}
