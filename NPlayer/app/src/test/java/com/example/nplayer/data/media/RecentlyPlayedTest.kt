package com.example.nplayer.data.media

import org.junit.Assert.assertEquals
import org.junit.Test

/** Ordering and de-duplication behind the Recently played list. */
class RecentlyPlayedTest {

    @Test
    fun `a first play lands at the front`() {
        assertEquals(
            listOf("a"),
            recentlyPlayedOrder(existing = emptyList(), playedKey = "a", limit = 20),
        )
    }

    @Test
    fun `a new play goes in front of the ones already there`() {
        assertEquals(
            listOf("c", "a", "b"),
            recentlyPlayedOrder(existing = listOf("a", "b"), playedKey = "c", limit = 20),
        )
    }

    @Test
    fun `replaying an item moves it to the front instead of duplicating it`() {
        assertEquals(
            listOf("b", "a", "c"),
            recentlyPlayedOrder(existing = listOf("a", "b", "c"), playedKey = "b", limit = 20),
        )
    }

    @Test
    fun `the list is capped at the limit, dropping the oldest`() {
        // The existing list is newest-first, so "a" is the newest of the three and the
        // cap drops "c", the one played longest ago.
        assertEquals(
            listOf("d", "a", "b"),
            recentlyPlayedOrder(existing = listOf("a", "b", "c"), playedKey = "d", limit = 3),
        )
    }

    @Test
    fun `replaying an item already at the front leaves the order alone`() {
        assertEquals(
            listOf("a", "b"),
            recentlyPlayedOrder(existing = listOf("a", "b"), playedKey = "a", limit = 20),
        )
    }

    @Test
    fun `a non-positive limit keeps nothing`() {
        assertEquals(
            emptyList<String>(),
            recentlyPlayedOrder(existing = listOf("a"), playedKey = "b", limit = 0),
        )
    }
}
