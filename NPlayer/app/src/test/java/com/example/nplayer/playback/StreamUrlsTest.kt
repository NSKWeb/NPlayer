package com.example.nplayer.playback

import com.example.nplayer.data.remote.model.MediaSourceInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamUrlsTest {

    private val server = "https://media.example.com"
    private val token = "abc123"

    @Test
    fun `direct plays mp4 with static flag and media source id`() {
        val source = MediaSourceInfo(id = "src-1", container = "mp4")
        val url = StreamUrls.streamUrl(server, token, "item-1", source)
        assertEquals(
            "https://media.example.com/Videos/item-1/stream?static=true&mediaSourceId=src-1&api_key=abc123",
            url,
        )
    }

    @Test
    fun `falls back to hls transcode for unknown container`() {
        val source = MediaSourceInfo(id = "src-2", container = "avi")
        val url = StreamUrls.streamUrl(server, token, "item-2", source)
        assertTrue(url.startsWith("https://media.example.com/Videos/item-2/master.m3u8"))
        assertTrue(url.contains("mediaSourceId=src-2"))
        assertTrue(url.contains("transcodingProtocol=hls"))
    }

    @Test
    fun `hls transcode when media source is null`() {
        val url = StreamUrls.streamUrl(server, token, "item-3", null)
        assertTrue(url.startsWith("https://media.example.com/Videos/item-3/master.m3u8"))
        assertTrue(url.contains("mediaSourceId=item-3"))
    }

    @Test
    fun `trailing slash on server url is normalised`() {
        val source = MediaSourceInfo(id = "src-1", container = "mkv")
        val url = StreamUrls.streamUrl("https://media.example.com/", token, "item-1", source)
        assertEquals(
            "https://media.example.com/Videos/item-1/stream?static=true&mediaSourceId=src-1&api_key=abc123",
            url,
        )
    }

    @Test
    fun `subtitle url targets the vtt endpoint`() {
        val url = StreamUrls.subtitleUrl(server, token, "item-1", "src-1", 3)
        assertEquals(
            "https://media.example.com/Videos/item-1/src-1/Subtitles/3/Stream.vtt?api_key=abc123",
            url,
        )
    }

    @Test
    fun `image url is null when tag missing`() {
        assertNull(StreamUrls.imageUrl(server, "item-1", null))
        assertNull(StreamUrls.imageUrl(server, "item-1", ""))
    }

    @Test
    fun `image url includes tag and sizing`() {
        val url = StreamUrls.imageUrl(server, "item-1", "tag-9")
        assertEquals(
            "https://media.example.com/Items/item-1/Images/Primary?tag=tag-9&maxWidth=480&quality=90",
            url,
        )
    }
}
