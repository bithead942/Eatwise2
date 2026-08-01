package com.bithead942.mealreminder.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.bithead942.mealreminder.appContainer
import com.bithead942.mealreminder.domain.ScheduleEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Handles the "I ATE" and "SNOOZE" buttons on a meal alert. */
class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val mealId = intent.getIntExtra(AlarmReceiver.EXTRA_MEAL_ID, -1)
        if (mealId < 0) return
        val minutes = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, 0)
        val action = intent.action ?: return
        val container = context.appContainer()
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                val now = System.currentTimeMillis()
                val state = container.repository.update(now) { current ->
                    when (action) {
                        ACTION_COMPLETE -> ScheduleEngine.markCompleted(current, mealId, now)
                        ACTION_SNOOZE -> ScheduleEngine.snooze(current, mealId, minutes, now)
                        else -> current
                    }
                }
                container.notifier.cancelMeal(mealId)
                container.alarmScheduler.sync(state, now)
                container.notifier.updateServiceNotification(state)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_COMPLETE = "com.bithead942.mealreminder.COMPLETE"
        const val ACTION_SNOOZE = "com.bithead942.mealreminder.SNOOZE"
        const val EXTRA_SNOOZE_MINUTES = "snooze_minutes"
    }
}
