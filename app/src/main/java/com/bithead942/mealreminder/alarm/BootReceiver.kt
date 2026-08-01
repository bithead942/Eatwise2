package com.bithead942.mealreminder.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Re-arms alarms and restarts the resident service after a reboot, update or clock change. */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        ReminderService.start(context)
    }
}
