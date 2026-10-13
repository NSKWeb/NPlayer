package com.example.nplayer.data.media

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads the device's own media through [MediaStore]. No network, no server: this is
 * the app's entire content source.
 */
@Singleton
class MediaStoreRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /** Everything the device exposes, newest first. */
    suspend fun scan(): List<MediaItem> = withContext(Dispatchers.IO) {
        (queryVideos() + queryAudio()).sortedByDescending { it.dateAddedSeconds }
    }

    private fun queryVideos(): List<MediaItem> {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
        )

        return runQuery(collection, projection) { cursor ->
            val id = cursor.getLong(0)
            val name = cursor.getStringOrNull(1)
            val width = cursor.getIntOrNull(5)
            val height = cursor.getIntOrNull(6)
            MediaItem(
                id = id,
                uri = ContentUris.withAppendedId(collection, id),
                title = name?.substringBeforeLast('.') ?: "Untitled",
                kind = MediaKind.VIDEO,
                durationMillis = cursor.getLongOrNull(2) ?: 0L,
                sizeBytes = cursor.getLongOrNull(3) ?: 0L,
                dateAddedSeconds = cursor.getLongOrNull(4) ?: 0L,
                resolution = if (width != null && height != null && height > 0) "${width}x$height" else null,
            )
        }
    }

    private fun queryAudio(): List<MediaItem> {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ARTIST,
        )

        return runQuery(collection, projection) { cursor ->
            val id = cursor.getLong(0)
            MediaItem(
                id = id,
                uri = ContentUris.withAppendedId(collection, id),
                title = cursor.getStringOrNull(1) ?: "Untitled",
                kind = MediaKind.AUDIO,
                durationMillis = cursor.getLongOrNull(2) ?: 0L,
                sizeBytes = cursor.getLongOrNull(3) ?: 0L,
                dateAddedSeconds = cursor.getLongOrNull(4) ?: 0L,
                album = cursor.getStringOrNull(5),
                artist = cursor.getStringOrNull(6),
            )
        }
    }

    /**
     * Shared cursor handling. [MediaStore.Audio.Media.IS_MUSIC] is deliberately not
     * filtered on, so short clips and recordings show up too.
     */
    private fun <T> runQuery(
        collection: android.net.Uri,
        projection: Array<String>,
        map: (android.database.Cursor) -> T,
    ): List<T> {
        val results = mutableListOf<T>()
        runCatching {
            context.contentResolver.query(
                collection,
                projection,
                null,
                null,
                "${MediaStore.MediaColumns.DATE_ADDED} DESC",
            )?.use { cursor ->
                while (cursor.moveToNext()) results += map(cursor)
            }
        }
        return results
    }
}

private fun android.database.Cursor.getStringOrNull(index: Int): String? =
    if (isNull(index)) null else getString(index)

private fun android.database.Cursor.getLongOrNull(index: Int): Long? =
    if (isNull(index)) null else getLong(index)

private fun android.database.Cursor.getIntOrNull(index: Int): Int? =
    if (isNull(index)) null else getInt(index)
