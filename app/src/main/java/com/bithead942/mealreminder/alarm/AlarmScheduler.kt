package com.bithead942.mealreminder.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.bithead942.mealreminder.data.AppState
import com.bithead942.mealreminder.domain.ScheduleEngine
import java.time.ZoneId

/**
 * Arms one exact alarm per pending meal. Overdue meals are re-armed every
 * [com.bithead942.mealreminder.data.Settings.repeatMinutes] minutes until they are marked as eaten.
 */
class AlarmScheduler(private val context: Context) {

    private val alarmManager: AlarmManager = context.getSystemService(AlarmManager::class.java)

    fun sync(state: AppState, now: Long = System.currentTimeMillis()) {
        val maxId = state.meals.maxOfOrNull { it.id } ?: 0
        for (id in 1..(maxId + STALE_ID_MARGIN)) {
            alarmManager.cancel(mealIntent(id))
        }
        state.meals.forEach { meal ->
            val at = ScheduleEngine.nextAlertAt(meal, state.settings, now) ?: return@forEach
            setExact(at, mealIntent(meal.id))
        }
        setExact(ScheduleEngine.startOfNextDay(now, ZoneId.systemDefault()), rollOverIntent())
    }

    fun cancelMeal(mealId: Int) {
        alarmManager.cancel(mealIntent(mealId))
    }

    private fun setExact(triggerAt: Long, operation: PendingIntent) {
        val canBeExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        if (canBeExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
        }
    }

    private fun mealIntent(mealId: Int): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_MEAL_ALERT
            putExtra(AlarmReceiver.EXTRA_MEAL_ID, mealId)
        }
        return PendingIntent.getBroadcast(
            context,
            mealId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun rollOverIntent(): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_DAY_ROLL_OVER
        }
        return PendingIntent.getBroadcast(
            context,
            ROLL_OVER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private companion object {
        const val ROLL_OVER_REQUEST_CODE = 90_001
        const val STALE_ID_MARGIN = 8
    }
}
