package com.example.nplayer.playback

import com.example.nplayer.data.remote.model.MediaSourceInfo

/**
 * Pure URL construction for Jellyfin playback. Kept free of Android/DI so the
 * logic can be unit tested directly.
 */
object StreamUrls {

    private val DIRECT_PLAY_CONTAINERS = setOf(
        "mp4", "mkv", "webm", "m4v", "mov",
        "mp3", "aac", "flac", "ogg", "opus", "wav", "m4a",
    )

    /**
     * Direct-plays the original file when the container is one ExoPlayer handles
     * well, otherwise asks Jellyfin for an adaptive HLS transcode.
     */
    fun streamUrl(serverUrl: String, token: String, itemId: String, mediaSource: MediaSourceInfo?): String {
        val server = serverUrl.trimEnd('/')
        val container = mediaSource?.container?.lowercase()
        val mediaId = mediaSource?.id
        val directPlayable = container != null && container in DIRECT_PLAY_CONTAINERS

        return if (directPlayable && !mediaId.isNullOrBlank()) {
            "$server/Videos/$itemId/stream?static=true&mediaSourceId=$mediaId&api_key=$token"
        } else {
            buildString {
                append("$server/Videos/$itemId/master.m3u8")
                append("?api_key=$token")
                append("&mediaSourceId=${mediaId ?: itemId}")
                append("&videoCodec=h264")
                append("&audioCodec=aac")
                append("&maxAudioChannels=2")
                append("&transcodingContainer=ts")
                append("&transcodingProtocol=hls")
            }
        }
    }

    /** Absolute URL for a subtitle stream Jellyfin delivers as WebVTT. */
    fun subtitleUrl(serverUrl: String, token: String, itemId: String, mediaId: String, streamIndex: Int): String {
        val server = serverUrl.trimEnd('/')
        return "$server/Videos/$itemId/$mediaId/Subtitles/$streamIndex/Stream.vtt?api_key=$token"
    }

    fun imageUrl(serverUrl: String, itemId: String, tag: String?): String? {
        if (tag.isNullOrBlank()) return null
        val server = serverUrl.trimEnd('/')
        return "$server/Items/$itemId/Images/Primary?tag=$tag&maxWidth=480&quality=90"
    }
}
