package com.raksha.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Receives BOOT_COMPLETED but does NOT auto-activate Shield.
 * Shield must be explicitly enabled by the user each time.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Boot completed — Shield remains off until user activates")
        }
    }
}
