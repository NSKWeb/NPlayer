package com.example.nplayer.downloads

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService
import com.example.nplayer.R

/** Creates the notification channels Media3 needs before downloads can start. */
fun createDownloadChannels(context: Context) {
    val manager = context.getSystemService<NotificationManager>() ?: return
    val channel = NotificationChannel(
        DownloadModule.CHANNEL_ID,
        context.getString(R.string.app_name),
        NotificationManager.IMPORTANCE_LOW,
    ).apply {
        description = context.getString(R.string.app_name)
        setShowBadge(false)
    }
    manager.createNotificationChannel(channel)
}
