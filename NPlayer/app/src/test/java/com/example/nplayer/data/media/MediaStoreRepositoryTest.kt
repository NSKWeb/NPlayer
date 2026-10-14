package com.example.nplayer.data.media

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver

/**
 * Exercises [MediaStoreRepository] against a fake MediaStore provider, so the cursor
 * mapping is verified for real instead of only by compilation. No device is involved.
 */
@RunWith(RobolectricTestRunner::class)
class MediaStoreRepositoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    /** Serves canned rows, projecting them through whatever columns are requested. */
    private class FakeMediaProvider(
        private val videoRows: List<Map<String, Any?>>,
        private val audioRows: List<Map<String, Any?>>,
    ) : ContentProvider() {

        override fun onCreate(): Boolean = true

        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?,
        ): Cursor {
            val rows = if (uri.pathSegments.contains("video")) videoRows else audioRows
            val columns = projection ?: emptyArray()
            val cursor = MatrixCursor(columns)
            rows.forEach { row -> cursor.addRow(columns.map { row[it] }.toTypedArray()) }
            return cursor
        }

        override fun getType(uri: Uri): String? = null
        override fun insert(uri: Uri, values: ContentValues?): Uri? = null
        override fun delete(uri: Uri, selection: String?, args: Array<out String>?): Int = 0
        override fun update(uri: Uri, v: ContentValues?, s: String?, a: Array<out String>?): Int = 0
    }

    private fun install(provider: FakeMediaProvider) {
        ShadowContentResolver.registerProviderInternal("media", provider)
    }

    private fun videoRow(
        id: Long,
        name: String,
        folder: Pair<String, Any?>,
        width: Int = 1920,
        height: Int = 1080,
    ) = mapOf(
        MediaStore.Video.Media._ID to id,
        MediaStore.Video.Media.DISPLAY_NAME to name,
        MediaStore.Video.Media.DURATION to 60_000L,
        MediaStore.Video.Media.SIZE to 1024L,
        MediaStore.Video.Media.DATE_ADDED to 1_700_000_000L,
        MediaStore.Video.Media.WIDTH to width,
        MediaStore.Video.Media.HEIGHT to height,
        folder.first to folder.second,
    )

    private fun audioRow(id: Long, title: String, folder: Pair<String, Any?>) = mapOf(
        MediaStore.Audio.Media._ID to id,
        MediaStore.Audio.Media.TITLE to title,
        MediaStore.Audio.Media.DURATION to 180_000L,
        MediaStore.Audio.Media.SIZE to 2048L,
        MediaStore.Audio.Media.DATE_ADDED to 1_700_000_100L,
        MediaStore.Audio.Media.ALBUM to "Album",
        MediaStore.Audio.Media.ARTIST to "Artist",
        folder.first to folder.second,
    )

    @Test
    @Config(sdk = [29])
    fun `maps video and audio rows from relative paths`() = runBlocking {
        install(
            FakeMediaProvider(
                videoRows = listOf(
                    videoRow(1, "Trip.mp4", MediaStore.MediaColumns.RELATIVE_PATH to "Movies/2024/"),
                ),
                audioRows = listOf(
                    audioRow(2, "Song", MediaStore.MediaColumns.RELATIVE_PATH to "Music/"),
                ),
            ),
        )

        val items = MediaStoreRepository(context).scan()

        val video = items.first { it.kind == MediaKind.VIDEO }
        assertEquals("Trip", video.title)
        assertEquals("Movies/2024", video.folder)
        assertEquals("1920x1080", video.resolution)

        val audio = items.first { it.kind == MediaKind.AUDIO }
        assertEquals("Song", audio.title)
        assertEquals("Music", audio.folder)
        assertEquals("Artist", audio.artist)
    }

    @Test
    @Config(sdk = [28])
    fun `derives folders from legacy absolute paths before API 29`() = runBlocking {
        install(
            FakeMediaProvider(
                videoRows = listOf(
                    videoRow(1, "Trip.mp4", MediaStore.MediaColumns.DATA to "/storage/emulated/0/Movies/2024/Trip.mp4"),
                ),
                audioRows = listOf(
                    audioRow(2, "Song", MediaStore.MediaColumns.DATA to "/storage/1234-5678/Music/Song.mp3"),
                ),
            ),
        )

        val items = MediaStoreRepository(context).scan()

        assertEquals("Movies/2024", items.first { it.kind == MediaKind.VIDEO }.folder)
        assertEquals("Music", items.first { it.kind == MediaKind.AUDIO }.folder)
    }

    @Test
    @Config(sdk = [29])
    fun `items with no folder land at the root`() = runBlocking {
        install(
            FakeMediaProvider(
                videoRows = listOf(
                    videoRow(1, "Loose.mp4", MediaStore.MediaColumns.RELATIVE_PATH to null),
                ),
                audioRows = emptyList(),
            ),
        )

        assertEquals("", MediaStoreRepository(context).scan().single().folder)
    }
}
