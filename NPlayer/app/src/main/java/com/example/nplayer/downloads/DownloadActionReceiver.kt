package com.example.nplayer.downloads

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.media3.common.util.UnstableApi

/** Receives pause/resume intents from download notifications. */
@UnstableApi
class DownloadActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Media3's own action receivers handle the standard actions; this hook
        // exists for custom actions we may add later (e.g. cancel-all).
    }
}
