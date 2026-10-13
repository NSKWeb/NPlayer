package com.example.nplayer.downloads

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.offline.DefaultDownloadIndex
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.Requirements
import com.example.nplayer.data.local.DownloadDao
import com.example.nplayer.data.local.DownloadEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the process-wide Media3 [DownloadManager] and mirrors its state into the
 * Room downloads table so the UI can observe progress without touching Media3.
 */
@UnstableApi
@Singleton
class DownloadModule @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val downloadDao: DownloadDao,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val executor = Executors.newFixedThreadPool(4)

    private val databaseProvider by lazy { StandaloneDatabaseProvider(context) }

    /** On-disk store for downloaded media. No eviction: the user chose to keep it. */
    private val downloadCache: SimpleCache by lazy {
        SimpleCache(
            File(context.getExternalFilesDir(null) ?: context.filesDir, "downloads"),
            NoOpCacheEvictor(),
            databaseProvider,
        )
    }

    val manager: DownloadManager by lazy {
        val httpDataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
        DownloadManager(
            context,
            databaseProvider,
            downloadCache,
            httpDataSourceFactory,
            executor,
        ).apply {
            maxParallelDownloads = 2
            setRequirements(Requirements(Requirements.NETWORK_UNMETERED))
        }
    }

    val notificationsHelper: DownloadNotificationHelper by lazy {
        DownloadNotificationHelper(context, CHANNEL_ID)
    }

    init {
        instance = this
        // Ensure the service is running so queued downloads execute.
        DownloadService.sendSetRequirements(
            context,
            NPlayerDownloadService::class.java,
            Requirements(Requirements.NETWORK_UNMETERED),
            /* foreground = */ false,
        )
        observeDownloads()
    }

    /** Mirror Media3 download states into Room so the UI can observe progress. */
    private fun observeDownloads() {
        scope.launch {
            val listener = object : DownloadManager.Listener {
                override fun onDownloadChanged(
                    downloadManager: DownloadManager,
                    download: Download,
                    finalException: Exception?,
                ) {
                    scope.launch { downloadDao.upsert(download.toEntity()) }
                }

                override fun onDownloadRemoved(
                    downloadManager: DownloadManager,
                    download: Download,
                ) {
                    scope.launch { downloadDao.delete(download.request.id) }
                }
            }
            manager.addListener(listener)
            // Seed the table with whatever is already known to Media3.
            val existing = mutableListOf<Download>()
            manager.downloadIndex.getDownloads().use { cursor ->
                while (cursor.moveToNext()) existing.add(cursor.download)
            }
            downloadDao.upsertAll(existing.map { it.toEntity() })
        }
    }

    private fun Download.toEntity() = DownloadEntity(
        itemId = request.id,
        name = "",
        imageTag = null,
        mediaId = request.data.toString(),
        state = state,
        progressPercent = percentDownloaded.toInt(),
        bytesDownloaded = bytesDownloaded,
        updatedAt = System.currentTimeMillis(),
    )

    companion object {
        const val CHANNEL_ID = "nplayer_downloads"
        const val NOTIFICATION_ID = 4001
        const val NOTIFICATION_UPDATE_INTERVAL_MS = 1_000L
        val CHANNEL_NAME_RESOURCE_ID = com.example.nplayer.R.string.app_name
        val CHANNEL_DESCRIPTION_RESOURCE_ID = com.example.nplayer.R.string.app_name

        @Volatile private var instance: DownloadModule? = null

        fun manager(): DownloadManager = requireNotNull(instance).manager
        fun notificationsHelper(): DownloadNotificationHelper = requireNotNull(instance).notificationsHelper
    }
}
