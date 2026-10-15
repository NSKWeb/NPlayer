package com.example.nplayer.data.media

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import com.example.nplayer.data.lyrics.isLyricsExtension
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

    /**
     * Subtitle files on the device, keyed by lower-case file name. Sidecars are
     * matched to media by name at playback time, so the library is not rescanned when
     * one is added. Uses `MediaStore.Files`, which needs no extra permission beyond
     * the media read already granted.
     */
    suspend fun scanSubtitles(): Map<String, SubtitleFile> = withContext(Dispatchers.IO) {
        scanSidecars { name, uri, folder ->
            SubtitleFile(name = name, uri = uri, folder = folder)
        }.let { files ->
            files.associateBy { it.name.lowercase() }
        }
    }

    /**
     * Lyrics files on the device, keyed by lower-case file name, matched to audio by
     * name at playback time. Read through the same `MediaStore.Files` query as the
     * subtitles, so no extra permission is needed.
     */
    suspend fun scanLyrics(): Map<String, LyricsFile> = withContext(Dispatchers.IO) {
        scanSidecars { name, uri, folder ->
            LyricsFile(name = name, uri = uri, folder = folder)
        }.let { files ->
            files.associateBy { it.name.lowercase() }
        }
    }

    /**
     * Reads every subtitle and lyrics file on the device in one pass. [map] turns a
     * matching row into whichever sidecar type is wanted; non-matching extensions are
     * skipped before it is called.
     */
    private fun <T> scanSidecars(
        map: (name: String, uri: android.net.Uri, folder: String) -> T,
    ): List<T> {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Files.getContentUri("external")
        }
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
        ) + folderColumns

        val results = mutableListOf<T>()
        runQuery(collection, projection) { cursor ->
            val id = cursor.getLongOrNull(0) ?: return@runQuery
            val name = cursor.getStringOrNull(1) ?: return@runQuery
            val extension = name.substringAfterLast('.', "")
            if (isSubtitleExtension(extension) || isLyricsExtension(extension)) {
                results += map(name, ContentUris.withAppendedId(collection, id), cursor.folderOrRoot(2))
            }
        }
        return results
    }

    /**
     * `RELATIVE_PATH` only exists from API 29. Asking for an unknown column throws,
     * which [runQuery] would swallow into an empty library, so the column is requested
     * only where it exists. On API 26–28 the folder is derived from the legacy `DATA`
     * column instead, so older devices still get a real tree.
     */
    private val hasRelativePath: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    private val folderColumns: Array<String>
        get() = if (hasRelativePath) {
            arrayOf(MediaStore.MediaColumns.RELATIVE_PATH)
        } else {
            arrayOf(MediaStore.MediaColumns.DATA)
        }

    private fun android.database.Cursor.folderOrRoot(index: Int): String =
        if (hasRelativePath) {
            normalizeFolder(getStringOrNull(index))
        } else {
            folderFromAbsolutePath(getStringOrNull(index))
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
        ) + folderColumns

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
                folder = cursor.folderOrRoot(7),
                fileName = name.orEmpty(),
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
            MediaStore.Audio.Media.DISPLAY_NAME,
        ) + folderColumns

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
                folder = cursor.folderOrRoot(8),
                fileName = cursor.getStringOrNull(7).orEmpty(),
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
