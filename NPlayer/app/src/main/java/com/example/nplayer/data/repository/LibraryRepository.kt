package com.example.nplayer.data.repository

import com.example.nplayer.data.prefs.SessionStore
import com.example.nplayer.data.remote.JellyfinApi
import com.example.nplayer.data.remote.model.BaseItemDto
import com.example.nplayer.data.remote.model.BaseItemDtoQueryResult
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryRepository @Inject constructor(
    private val api: JellyfinApi,
    private val sessionStore: SessionStore,
) {
    private suspend fun userId(): String =
        sessionStore.current().userId ?: error("Not authenticated")

    suspend fun libraries(): ApiResult<BaseItemDtoQueryResult> = apiCall {
        api.userViews(userId())
    }

    suspend fun items(
        parentId: String?,
        includeItemTypes: String? = "Movie,Series",
        startIndex: Int = 0,
        limit: Int = 60,
    ): ApiResult<BaseItemDtoQueryResult> = apiCall {
        api.items(
            userId = userId(),
            parentId = parentId,
            includeItemTypes = includeItemTypes,
            recursive = true,
            startIndex = startIndex,
            limit = limit,
        )
    }

    /** A "latest" style rail: newest items, unsorted by type. */
    suspend fun latest(parentId: String? = null, limit: Int = 20): ApiResult<BaseItemDtoQueryResult> =
        apiCall {
            api.items(
                userId = userId(),
                parentId = parentId,
                includeItemTypes = "Movie,Series,Episode",
                recursive = true,
                sortBy = "DateCreated",
                sortOrder = "Descending",
                limit = limit,
            )
        }

    suspend fun item(itemId: String): ApiResult<BaseItemDto> = apiCall {
        api.item(userId(), itemId)
    }

    /** Direct children of a series (seasons) or season (episodes). */
    suspend fun children(parentId: String, includeItemTypes: String): ApiResult<BaseItemDtoQueryResult> =
        apiCall {
            api.items(
                userId = userId(),
                parentId = parentId,
                includeItemTypes = includeItemTypes,
                recursive = false,
                limit = 200,
            )
        }

    suspend fun search(term: String): ApiResult<BaseItemDtoQueryResult> = apiCall {
        api.search(userId(), term)
    }

    suspend fun markFavorite(itemId: String, favorite: Boolean): ApiResult<Unit> = apiCall {
        if (favorite) api.markFavorite(itemId) else api.unmarkFavorite(itemId)
    }

    suspend fun isAuthenticated(): Boolean = sessionStore.current().isAuthenticated

    suspend fun currentSessionOrNull() = sessionStore.session.first()
}
