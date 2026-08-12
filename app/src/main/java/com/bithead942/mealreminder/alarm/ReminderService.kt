package com.bithead942.mealreminder.alarm

import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.bithead942.mealreminder.appContainer
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Keeps the app resident so meal alarms stay armed while the UI is not running. The ongoing
 * notification shows the next scheduled meal and the service re-arms alarms whenever state changes.
 */
class ReminderService : LifecycleService() {

    override fun onCreate() {
        super.onCreate()
        val container = appContainer()
        startForegroundCompat()
        lifecycleScope.launch {
            container.repository.state.collectLatest { state ->
                container.alarmScheduler.sync(state)
                container.notifier.updateServiceNotification(state)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startForegroundCompat()
        return START_STICKY
    }

    private fun startForegroundCompat() {
        val notification = appContainer().notifier.buildServiceNotification(null)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                ReminderNotifier.SERVICE_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(ReminderNotifier.SERVICE_NOTIFICATION_ID, notification)
        }
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, ReminderService::class.java)
            runCatching { context.startForegroundService(intent) }
        }
    }
}
