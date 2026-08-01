package com.bithead942.mealreminder.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.bithead942.mealreminder.appContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Fires at a meal time (and every repeat interval afterwards) and at midnight for the day reset. */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val container = context.appContainer()
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                val now = System.currentTimeMillis()
                val state = container.repository.update(now) { it }
                when (intent.action) {
                    ACTION_MEAL_ALERT -> {
                        val mealId = intent.getIntExtra(EXTRA_MEAL_ID, -1)
                        val index = state.meals.indexOfFirst { it.id == mealId }
                        val meal = state.meals.getOrNull(index)
                        if (meal != null && !meal.isCompleted && !meal.isSnoozed(now) && meal.isDue(now)) {
                            container.notifier.notifyMeal(meal, index + 1, state.settings, now)
                            container.repository.update(now) { current ->
                                current.copy(
                                    meals = current.meals.map {
                                        if (it.id == mealId) it.copy(alerting = true) else it
                                    }
                                )
                            }
                        }
                    }

                    ACTION_DAY_ROLL_OVER -> container.notifier.cancelAllMealAlerts(state)
                }
                container.alarmScheduler.sync(container.repository.current(), System.currentTimeMillis())
                container.notifier.updateServiceNotification(container.repository.current())
                ReminderService.start(context)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_MEAL_ALERT = "com.bithead942.mealreminder.MEAL_ALERT"
        const val ACTION_DAY_ROLL_OVER = "com.bithead942.mealreminder.DAY_ROLL_OVER"
        const val EXTRA_MEAL_ID = "meal_id"
    }
}
