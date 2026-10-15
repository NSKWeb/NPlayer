package com.example.nplayer.data.lyrics

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads and parses a lyrics sidecar. The file is a `content://` URI on the device, so
 * there is nothing to fetch and nothing to cache: it is read once when the track opens.
 */
@Singleton
class LyricsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun load(uri: Uri): Lyrics = withContext(Dispatchers.IO) {
        val text = runCatching {
            context.contentResolver.openInputStream(uri)
                ?.bufferedReader()
                ?.use { it.readText() }
        }.getOrNull()
        text?.let(::parseLrc) ?: Lyrics()
    }
}
