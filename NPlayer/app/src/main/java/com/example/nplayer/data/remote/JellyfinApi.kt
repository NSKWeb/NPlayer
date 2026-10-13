package com.example.nplayer.data.remote

import com.example.nplayer.data.remote.model.AuthenticationRequest
import com.example.nplayer.data.remote.model.AuthenticationResult
import com.example.nplayer.data.remote.model.BaseItemDtoQueryResult
import com.example.nplayer.data.remote.model.PlaybackInfoResponse
import com.example.nplayer.data.remote.model.PlaybackProgressInfo
import com.example.nplayer.data.remote.model.PlaybackStopInfo
import com.example.nplayer.data.remote.model.PublicSystemInfo
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Minimal Jellyfin REST surface used by N Player.
 *
 * The base URL is supplied at runtime from the user's server, so this interface
 * only declares relative paths.
 */
interface JellyfinApi {

    @GET("System/Info/Public")
    suspend fun publicSystemInfo(): PublicSystemInfo

    @POST("Users/AuthenticateByName")
    suspend fun authenticateByName(@Body body: AuthenticationRequest): AuthenticationResult

    /** Top-level libraries the user can see. */
    @GET("Users/{userId}/Views")
    suspend fun userViews(@Path("userId") userId: String): BaseItemDtoQueryResult

    /** Items within a library / folder, or a filtered listing. */
    @GET("Users/{userId}/Items")
    suspend fun items(
        @Path("userId") userId: String,
        @Query("parentId") parentId: String? = null,
        @Query("includeItemTypes") includeItemTypes: String? = null,
        @Query("recursive") recursive: Boolean? = null,
        @Query("sortBy") sortBy: String = "SortName",
        @Query("sortOrder") sortOrder: String = "Ascending",
        @Query("startIndex") startIndex: Int = 0,
        @Query("limit") limit: Int = 60,
        @Query("fields") fields: String = "Overview,MediaSources,MediaStreams,PrimaryImageAspectRatio",
        @Query("imageTypeLimit") imageTypeLimit: Int = 1,
        @Query("enableImageTypes") enableImageTypes: String = "Primary,Backdrop,Thumb",
    ): BaseItemDtoQueryResult

    @GET("Users/{userId}/Items/{itemId}")
    suspend fun item(
        @Path("userId") userId: String,
        @Path("itemId") itemId: String,
    ): com.example.nplayer.data.remote.model.BaseItemDto

    @GET("Users/{userId}/Items")
    suspend fun search(
        @Path("userId") userId: String,
        @Query("searchTerm") searchTerm: String,
        @Query("recursive") recursive: Boolean = true,
        @Query("includeItemTypes") includeItemTypes: String = "Movie,Series,Episode,Video",
        @Query("limit") limit: Int = 40,
        @Query("fields") fields: String = "Overview,PrimaryImageAspectRatio",
    ): BaseItemDtoQueryResult

    /** Ask the server how to play an item (direct play vs. transcode). */
    @POST("Items/{itemId}/PlaybackInfo")
    suspend fun playbackInfo(
        @Path("itemId") itemId: String,
        @Query("userId") userId: String,
        @Body body: Map<String, String> = emptyMap(),
    ): PlaybackInfoResponse

    @POST("Sessions/Playing")
    suspend fun reportPlaybackStart(@Body body: PlaybackProgressInfo)

    @POST("Sessions/Playing/Progress")
    suspend fun reportPlaybackProgress(@Body body: PlaybackProgressInfo)

    @POST("Sessions/Playing/Stopped")
    suspend fun reportPlaybackStopped(@Body body: PlaybackStopInfo)

    @POST("UserItems/{itemId}/Favorite")
    suspend fun markFavorite(@Path("itemId") itemId: String)

    @DELETE("UserItems/{itemId}/Favorite")
    suspend fun unmarkFavorite(@Path("itemId") itemId: String)
}
