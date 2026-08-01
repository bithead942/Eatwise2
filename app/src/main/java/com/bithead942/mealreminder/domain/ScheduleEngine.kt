package com.bithead942.mealreminder.domain

import com.bithead942.mealreminder.data.AppState
import com.bithead942.mealreminder.data.MealReminder
import com.bithead942.mealreminder.data.Settings
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Pure scheduling rules for the meal plan. Kept free of Android dependencies so the behaviour can
 * be unit tested.
 *
 * The day starts with a blank schedule. Times appear once an anchor exists: either the user marked
 * a meal as eaten, or pinned a time on a meal manually. Every pending meal after an anchor is
 * spaced by [Settings.intervalMinutes], so completing a meal late pushes the rest of the day back
 * by the same amount.
 */
object ScheduleEngine {

    fun reschedule(meals: List<MealReminder>, settings: Settings): List<MealReminder> {
        val stepMillis = settings.intervalMinutes.toLong() * 60_000L
        var anchor: Long? = null
        var stepsSinceAnchor = 0
        return meals.map { meal ->
            when {
                meal.completedAt != null -> {
                    anchor = meal.completedAt
                    stepsSinceAnchor = 0
                    meal
                }

                meal.manualAt != null -> {
                    anchor = meal.manualAt
                    stepsSinceAnchor = 0
                    meal.copy(scheduledAt = meal.manualAt)
                }

                anchor != null -> {
                    stepsSinceAnchor++
                    meal.copy(scheduledAt = anchor!! + stepsSinceAnchor * stepMillis)
                }

                else -> meal.copy(scheduledAt = null)
            }
        }
    }

    fun markCompleted(state: AppState, mealId: Int, now: Long): AppState {
        val meals = state.meals.map { meal ->
            if (meal.id == mealId) {
                meal.copy(completedAt = now, snoozedUntil = null, alerting = false)
            } else {
                meal
            }
        }
        return state.copy(meals = reschedule(meals, state.settings))
    }

    fun markPending(state: AppState, mealId: Int): AppState {
        val meals = state.meals.map { meal ->
            if (meal.id == mealId) meal.copy(completedAt = null, alerting = false) else meal
        }
        return state.copy(meals = reschedule(meals, state.settings))
    }

    fun snooze(state: AppState, mealId: Int, minutes: Int, now: Long): AppState {
        val meals = state.meals.map { meal ->
            if (meal.id == mealId) {
                meal.copy(snoozedUntil = now + minutes * 60_000L, alerting = false)
            } else {
                meal
            }
        }
        return state.copy(meals = meals)
    }

    /** Pins [timeOfDay] on a meal; later pending meals are spaced from it. */
    fun setManualTime(state: AppState, mealId: Int, timeOfDay: LocalTime, now: Long, zone: ZoneId): AppState {
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val millis = today.atTime(timeOfDay).atZone(zone).toInstant().toEpochMilli()
        val meals = state.meals.map { meal ->
            if (meal.id == mealId) {
                meal.copy(manualAt = millis, scheduledAt = millis, snoozedUntil = null)
            } else {
                meal
            }
        }
        return state.copy(meals = reschedule(meals, state.settings))
    }

    fun addMeal(state: AppState): AppState {
        val nextId = (state.meals.maxOfOrNull { it.id } ?: 0) + 1
        val meals = state.meals + MealReminder(id = nextId)
        return state.copy(meals = reschedule(meals, state.settings))
    }

    fun removeMeal(state: AppState, mealId: Int): AppState {
        if (state.meals.size <= 1) return state
        val meals = state.meals.filterNot { it.id == mealId }
        return state.copy(meals = reschedule(meals, state.settings))
    }

    fun setMealCount(state: AppState, count: Int): AppState {
        val target = count.coerceIn(1, 24)
        var meals = state.meals
        while (meals.size > target) {
            meals = meals.dropLast(1)
        }
        while (meals.size < target) {
            meals = meals + MealReminder(id = (meals.maxOfOrNull { it.id } ?: 0) + 1)
        }
        return state.copy(meals = reschedule(meals, state.settings))
    }

    fun updateSettings(state: AppState, settings: Settings): AppState =
        state.copy(settings = settings, meals = reschedule(state.meals, settings))

    /** Blanks the schedule when the calendar day changes. */
    fun rollOverDay(state: AppState, now: Long, zone: ZoneId): AppState {
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate().toEpochDay()
        if (today == state.dayEpochDay) return state
        val meals = state.meals.map { MealReminder(id = it.id) }
        return state.copy(meals = meals, dayEpochDay = today, waterOz = 0)
    }

    /**
     * Next moment an alert should fire for [meal], or null when no alert is pending.
     * Overdue meals repeat every [Settings.repeatMinutes] until they are marked as eaten.
     */
    fun nextAlertAt(meal: MealReminder, settings: Settings, now: Long): Long? {
        if (meal.isCompleted) return null
        val scheduledAt = meal.scheduledAt ?: return null
        meal.snoozedUntil?.let { if (it > now) return it }
        if (scheduledAt > now) return scheduledAt
        val repeatMillis = settings.repeatMinutes.toLong() * 60_000L
        if (repeatMillis <= 0L) return scheduledAt
        val elapsed = now - scheduledAt
        return scheduledAt + ((elapsed / repeatMillis) + 1) * repeatMillis
    }

    fun nextMeal(state: AppState, now: Long): MealReminder? =
        state.meals.filter { !it.isCompleted && it.scheduledAt != null }
            .minByOrNull { maxOf(it.scheduledAt!!, it.snoozedUntil ?: 0L) }
            ?: state.meals.firstOrNull { !it.isCompleted }

    fun startOfNextDay(now: Long, zone: ZoneId): Long {
        val date: LocalDate = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        return date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    }
}
