package com.example.nplayer.data

import android.content.Context
import coil.imageLoader
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Frees the on-device caches the app builds up: the Coil thumbnail cache plus
 * anything else sitting in the app's cache directory. Nothing here is needed to
 * play a file, so clearing it costs only the time to decode thumbnails again.
 */
@Singleton
class CacheManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /** Total size of the app cache directory, in bytes. */
    fun cacheSizeBytes(): Long = directorySize(context.cacheDir)

    /**
     * Drops the in-memory image cache, the Coil disk cache, and the rest of the
     * cache directory. Returns the number of bytes freed.
     */
    suspend fun clear(): Long = withContext(Dispatchers.IO) {
        val before = cacheSizeBytes()

        val loader = context.imageLoader
        loader.memoryCache?.clear()
        loader.diskCache?.clear()

        // The Coil cache lives under cacheDir, so this also picks up anything the
        // decoder or the media stack left behind.
        context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }

        (before - cacheSizeBytes()).coerceAtLeast(0)
    }
}

private fun directorySize(dir: File?): Long {
    if (dir == null || !dir.exists()) return 0
    return dir.walkBottomUp().filter { it.isFile }.sumOf { it.length() }
}

/** Renders a byte count as "0 B", "812 KB", or "1.4 MB". */
fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "${kb.round1()} KB"
    val mb = kb / 1024.0
    if (mb < 1024) return "${mb.round1()} MB"
    return "${(mb / 1024.0).round1()} GB"
}

/** One decimal place, but no trailing ".0". */
private fun Double.round1(): String {
    val rounded = Math.round(this * 10) / 10.0
    return if (rounded == rounded.toLong().toDouble()) {
        rounded.toLong().toString()
    } else {
        rounded.toString()
    }
}
