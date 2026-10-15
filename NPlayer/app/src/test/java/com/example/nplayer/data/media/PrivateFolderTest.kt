package com.example.nplayer.data.media

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import com.example.nplayer.data.prefs.AppSettingsStore
import com.example.nplayer.data.prefs.PlaybackSettingsStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver

/**
 * Verifies the private folder end to end: real settings store, real MediaStore
 * repository over a fake provider, and the real [MediaLibrary] filtering. No device.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class PrivateFolderTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private class FakeMediaProvider(
        private val rows: List<Map<String, Any?>>,
    ) : ContentProvider() {
        override fun onCreate(): Boolean = true

        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?,
        ): Cursor {
            val columns = projection ?: emptyArray()
            val cursor = MatrixCursor(columns)
            // Video queries are the only ones with rows in this test.
            if (uri.pathSegments.contains("video")) {
                rows.forEach { row -> cursor.addRow(columns.map { row[it] }.toTypedArray()) }
            }
            return cursor
        }

        override fun getType(uri: Uri): String? = null
        override fun insert(uri: Uri, values: ContentValues?): Uri? = null
        override fun delete(uri: Uri, selection: String?, args: Array<out String>?): Int = 0
        override fun update(uri: Uri, v: ContentValues?, s: String?, a: Array<out String>?): Int = 0
    }

    private fun videoRow(id: Long, name: String) = mapOf(
        MediaStore.Video.Media._ID to id,
        MediaStore.Video.Media.DISPLAY_NAME to name,
        MediaStore.Video.Media.DURATION to 60_000L,
        MediaStore.Video.Media.SIZE to 1024L,
        MediaStore.Video.Media.DATE_ADDED to 1_700_000_000L,
        MediaStore.Video.Media.WIDTH to 1920,
        MediaStore.Video.Media.HEIGHT to 1080,
        MediaStore.MediaColumns.RELATIVE_PATH to "Movies/",
    )

    @Test
    fun `a private item disappears from every listing but stays in allItems`() = runBlocking {
        ShadowContentResolver.registerProviderInternal(
            "media",
            FakeMediaProvider(listOf(videoRow(1, "Public.mp4"), videoRow(2, "Hidden.mp4"))),
        )

        val settingsStore = AppSettingsStore(context)
        val playbackSettings = PlaybackSettingsStore(context)
        val library = MediaLibrary(MediaStoreRepository(context), settingsStore, playbackSettings)
        library.refresh()

        assertEquals(2, library.videos().size)

        // The hidden key is the same `content://` URI the item is keyed by.
        val hidden = library.videos().first { it.title == "Hidden" }
        settingsStore.togglePrivate(hidden.key)
        // The library observes the settings flow on a background scope.
        Thread.sleep(500)

        assertEquals(listOf("Public"), library.videos().map { it.title })
        assertTrue(library.search("Hidden").isEmpty())
        assertEquals(1, library.foldersIn("").first { it.name == "Movies" }.itemCount)
        // The gated screen still sees it.
        assertTrue(library.allItems.value.any { it.title == "Hidden" })
    }
}
