package com.example.nplayer.playback

import android.net.Uri
import com.example.nplayer.data.media.MediaItem
import com.example.nplayer.data.media.MediaKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QueueBuilderTest {

    private fun item(
        name: String,
        kind: MediaKind = MediaKind.VIDEO,
        folder: String = "Movies",
        added: Long = 0,
    ) = MediaItem(
        id = name.hashCode().toLong(),
        uri = Uri.parse("content://media/$name"),
        title = name,
        kind = kind,
        durationMillis = 1_000,
        sizeBytes = 1_000,
        dateAddedSeconds = added,
        folder = folder,
    )

    @Test
    fun `queues the same folder newest first`() {
        val items = listOf(
            item("a.mp4", added = 10),
            item("b.mp4", added = 30),
            item("c.mp4", added = 20),
        )
        val queue = buildQueue(items, "content://media/b.mp4")
        assertEquals(listOf("b.mp4", "c.mp4", "a.mp4"), queue.map { it.title })
    }

    @Test
    fun `keeps video and audio apart`() {
        val items = listOf(
            item("a.mp4"),
            item("song.mp3", kind = MediaKind.AUDIO),
            item("b.mp4"),
        )
        val video = buildQueue(items, "content://media/a.mp4", MediaKind.VIDEO)
        assertEquals(listOf("a.mp4", "b.mp4"), video.map { it.title })
        val audio = buildQueue(items, "content://media/song.mp3", MediaKind.AUDIO)
        assertEquals(emptyList<String>(), audio.map { it.title })
    }

    @Test
    fun `does not cross folders`() {
        val items = listOf(
            item("a.mp4", folder = "Movies/2024"),
            item("b.mp4", folder = "Movies/2025"),
            item("c.mp4", folder = "Movies/2024"),
        )
        val queue = buildQueue(items, "content://media/a.mp4")
        assertEquals(listOf("a.mp4", "c.mp4"), queue.map { it.title })
    }

    @Test
    fun `treats a folder path with a trailing slash as the same folder`() {
        val items = listOf(
            item("a.mp4", folder = "Movies/"),
            item("b.mp4", folder = "Movies"),
        )
        assertEquals(2, buildQueue(items, "content://media/a.mp4").size)
    }

    @Test
    fun `returns empty for a lone item`() {
        val items = listOf(item("only.mp4"))
        assertEquals(emptyList<MediaItem>(), buildQueue(items, "content://media/only.mp4"))
    }

    @Test
    fun `returns empty for an unknown item`() {
        val items = listOf(item("a.mp4"), item("b.mp4"))
        assertEquals(emptyList<MediaItem>(), buildQueue(items, "content://media/missing.mp4"))
    }

    @Test
    fun `next and previous walk the queue`() {
        val items = listOf(
            item("a.mp4", added = 30),
            item("b.mp4", added = 20),
            item("c.mp4", added = 10),
        )
        val queue = buildQueue(items, "content://media/b.mp4")
        assertEquals("c.mp4", nextInQueue(queue, "content://media/b.mp4")?.title)
        assertEquals("a.mp4", previousInQueue(queue, "content://media/b.mp4")?.title)
        assertNull(nextInQueue(queue, "content://media/c.mp4"))
        assertNull(previousInQueue(queue, "content://media/a.mp4"))
    }

    @Test
    fun `queue index falls back to the start`() {
        val items = listOf(item("a.mp4"), item("b.mp4"))
        val queue = buildQueue(items, "content://media/a.mp4")
        assertEquals(1, queueIndexOf(queue, "content://media/b.mp4"))
        assertEquals(0, queueIndexOf(queue, "content://media/absent.mp4"))
    }
}
