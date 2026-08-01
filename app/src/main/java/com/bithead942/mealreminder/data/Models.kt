package com.bithead942.mealreminder.data

import kotlinx.serialization.Serializable

/**
 * A single meal slot for the current day.
 *
 * [scheduledAt] is null until the schedule has been seeded, which happens when the first meal of
 * the day is marked as eaten (or when the user pins a time manually via [manualAt]).
 */
@Serializable
data class MealReminder(
    val id: Int,
    val scheduledAt: Long? = null,
    val completedAt: Long? = null,
    val manualAt: Long? = null,
    val snoozedUntil: Long? = null,
    val alerting: Boolean = false
) {
    val isCompleted: Boolean get() = completedAt != null

    /** Time shown on the row: the completion time once eaten, otherwise the scheduled time. */
    val displayTime: Long? get() = completedAt ?: scheduledAt

    fun isSnoozed(now: Long): Boolean = snoozedUntil != null && snoozedUntil > now

    fun isDue(now: Long): Boolean = !isCompleted && scheduledAt != null && scheduledAt <= now
}

@Serializable
data class Settings(
    val intervalMinutes: Int = DEFAULT_INTERVAL_MINUTES,
    val repeatMinutes: Int = DEFAULT_REPEAT_MINUTES,
    val soundEnabled: Boolean = true,
    val vibrateEnabled: Boolean = true,
    /** System ringtone URI, or null for the device default alarm sound. */
    val soundUri: String? = null,
    val soundName: String? = null,
    val waterGoalOz: Int = DEFAULT_WATER_GOAL_OZ,
    val waterServingOz: Int = DEFAULT_WATER_SERVING_OZ
) {
    companion object {
        const val DEFAULT_INTERVAL_MINUTES = 150
        const val DEFAULT_REPEAT_MINUTES = 5
        const val DEFAULT_WATER_GOAL_OZ = 120
        const val DEFAULT_WATER_SERVING_OZ = 8
    }
}

@Serializable
data class AppState(
    val meals: List<MealReminder> = defaultMeals(),
    val settings: Settings = Settings(),
    /** Epoch day the schedule belongs to; used to blank the schedule at the start of a new day. */
    val dayEpochDay: Long = 0L,
    val waterOz: Int = 0
) {
    companion object {
        const val DEFAULT_MEAL_COUNT = 6

        fun defaultMeals(count: Int = DEFAULT_MEAL_COUNT): List<MealReminder> =
            (1..count).map { MealReminder(id = it) }
    }
}

const val SNOOZE_15 = 15
const val SNOOZE_20 = 20
const val SNOOZE_30 = 30

val SNOOZE_OPTIONS = listOf(SNOOZE_15, SNOOZE_20, SNOOZE_30)
