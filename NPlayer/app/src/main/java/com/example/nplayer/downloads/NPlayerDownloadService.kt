package com.example.nplayer.downloads

import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.Scheduler

/**
 * Foreground service that keeps Media3 downloads running in the background.
 * The manager and notifications helper are supplied by [DownloadModule].
 */
@UnstableApi
class NPlayerDownloadService : DownloadService(
    DownloadModule.NOTIFICATION_ID,
    DownloadModule.NOTIFICATION_UPDATE_INTERVAL_MS,
    DownloadModule.CHANNEL_ID,
    DownloadModule.CHANNEL_NAME_RESOURCE_ID,
    DownloadModule.CHANNEL_DESCRIPTION_RESOURCE_ID,
) {
    override fun getScheduler(): Scheduler? = null
    override fun getDownloadManager() = DownloadModule.manager()

    override fun getForegroundNotification(
        downloads: MutableList<androidx.media3.exoplayer.offline.Download>,
        notMetRequirements: Int,
    ) = DownloadModule.notificationsHelper()
        .buildProgressNotification(
            this,
            android.R.drawable.stat_sys_download,
            /* contentIntent = */ null,
            /* message = */ null,
            downloads,
            notMetRequirements,
        )
}
