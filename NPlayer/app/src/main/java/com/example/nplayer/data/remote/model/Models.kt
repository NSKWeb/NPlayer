package com.example.nplayer.data.remote.model

import com.google.gson.annotations.SerializedName

/** Public, unauthenticated server identity: GET /System/Info/Public */
data class PublicSystemInfo(
    @SerializedName("ServerName") val serverName: String? = null,
    @SerializedName("Version") val version: String? = null,
    @SerializedName("Id") val id: String? = null,
)

/** GET /Users/{userId}/Views — the top-level libraries. */
data class BaseItemDtoQueryResult(
    @SerializedName("Items") val items: List<BaseItemDto> = emptyList(),
    @SerializedName("TotalRecordCount") val totalRecordCount: Int = 0,
)

/** A Jellyfin library item (movie, series, episode, folder, …). */
data class BaseItemDto(
    @SerializedName("Id") val id: String,
    @SerializedName("Name") val name: String? = null,
    @SerializedName("Type") val type: String? = null,
    @SerializedName("MediaType") val mediaType: String? = null,
    @SerializedName("CollectionType") val collectionType: String? = null,
    @SerializedName("Overview") val overview: String? = null,
    @SerializedName("ProductionYear") val productionYear: Int? = null,
    @SerializedName("RunTimeTicks") val runTimeTicks: Long? = null,
    @SerializedName("IndexNumber") val indexNumber: Int? = null,
    @SerializedName("ParentIndexNumber") val parentIndexNumber: Int? = null,
    @SerializedName("SeriesName") val seriesName: String? = null,
    @SerializedName("SeriesId") val seriesId: String? = null,
    @SerializedName("SeasonId") val seasonId: String? = null,
    @SerializedName("SeasonName") val seasonName: String? = null,
    @SerializedName("ImageTags") val imageTags: Map<String, String>? = null,
    @SerializedName("BackdropImageTags") val backdropImageTags: List<String>? = null,
    @SerializedName("UserData") val userData: UserItemData? = null,
    @SerializedName("MediaSources") val mediaSources: List<MediaSourceInfo>? = null,
) {
    /** Duration in milliseconds, if known. */
    val runtimeMillis: Long?
        get() = runTimeTicks?.let { it / 10_000 }

    val primaryImageTag: String? get() = imageTags?.get("Primary")
}

/** Per-user state for an item (played, resume position, favourites). */
data class UserItemData(
    @SerializedName("PlaybackPositionTicks") val playbackPositionTicks: Long = 0,
    @SerializedName("PlayCount") val playCount: Int = 0,
    @SerializedName("IsFavorite") val isFavorite: Boolean = false,
    @SerializedName("Played") val played: Boolean = false,
) {
    val resumePositionMillis: Long get() = playbackPositionTicks / 10_000
}

data class MediaSourceInfo(
    @SerializedName("Id") val id: String? = null,
    @SerializedName("Name") val name: String? = null,
    @SerializedName("Container") val container: String? = null,
    @SerializedName("Path") val path: String? = null,
    @SerializedName("Protocol") val protocol: String? = null,
    @SerializedName("RunTimeTicks") val runTimeTicks: Long? = null,
    @SerializedName("MediaStreams") val mediaStreams: List<MediaStream>? = null,
)

data class MediaStream(
    @SerializedName("Index") val index: Int = 0,
    @SerializedName("Type") val type: String? = null,
    @SerializedName("Codec") val codec: String? = null,
    @SerializedName("Language") val language: String? = null,
    @SerializedName("DisplayTitle") val displayTitle: String? = null,
    @SerializedName("IsDefault") val isDefault: Boolean = false,
    @SerializedName("DeliveryUrl") val deliveryUrl: String? = null,
)

data class AuthenticationResult(
    @SerializedName("User") val user: UserDto,
    @SerializedName("AccessToken") val accessToken: String,
    @SerializedName("ServerId") val serverId: String? = null,
)

data class UserDto(
    @SerializedName("Id") val id: String,
    @SerializedName("Name") val name: String,
)

data class AuthenticationRequest(
    @SerializedName("Username") val username: String,
    @SerializedName("Pw") val password: String,
)

/**
 * POST /Items/{itemId}/PlaybackInfo — decides how the server wants us to play.
 * When [directPlayUrl] is present we can stream the file as-is.
 */
data class PlaybackInfoResponse(
    @SerializedName("MediaSources") val mediaSources: List<MediaSourceInfo> = emptyList(),
    @SerializedName("PlaySessionId") val playSessionId: String? = null,
)

/** POST /Sessions/Playing* bodies. */
data class PlaybackProgressInfo(
    @SerializedName("ItemId") val itemId: String,
    @SerializedName("PlaySessionId") val playSessionId: String? = null,
    @SerializedName("PositionTicks") val positionTicks: Long? = null,
    @SerializedName("IsPaused") val isPaused: Boolean? = null,
    @SerializedName("IsMuted") val isMuted: Boolean? = null,
    @SerializedName("CanSeek") val canSeek: Boolean = true,
)

data class PlaybackStopInfo(
    @SerializedName("ItemId") val itemId: String,
    @SerializedName("PlaySessionId") val playSessionId: String? = null,
    @SerializedName("PositionTicks") val positionTicks: Long? = null,
)
