package com.bithead942.mealreminder.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bithead942.mealreminder.alarm.AlarmScheduler
import com.bithead942.mealreminder.alarm.ReminderNotifier
import com.bithead942.mealreminder.appContainer
import com.bithead942.mealreminder.data.AppState
import com.bithead942.mealreminder.data.MealRepository
import com.bithead942.mealreminder.data.Settings
import com.bithead942.mealreminder.domain.ScheduleEngine
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MealViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MealRepository = application.appContainer().repository
    private val alarms: AlarmScheduler = application.appContainer().alarmScheduler
    private val notifier: ReminderNotifier = application.appContainer().notifier
    private val zone: ZoneId = ZoneId.systemDefault()

    val state: StateFlow<AppState> = repository.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppState())

    private val _now = MutableStateFlow(System.currentTimeMillis())

    /** Drives the "due" styling of the rows without needing a recomposition trigger elsewhere. */
    val now: StateFlow<Long> = _now.asStateFlow()

    init {
        viewModelScope.launch {
            while (true) {
                _now.value = System.currentTimeMillis()
                // Applying an identity update rolls the day over and re-spaces the schedule.
                repository.update(_now.value) { it }
                delay(TICK_MILLIS)
            }
        }
    }

    fun toggleMeal(mealId: Int) = mutate { state, now ->
        val meal = state.meals.firstOrNull { it.id == mealId } ?: return@mutate state
        if (meal.isCompleted) {
            ScheduleEngine.markPending(state, mealId)
        } else {
            ScheduleEngine.markCompleted(state, mealId, now)
        }
    }

    fun snooze(mealId: Int, minutes: Int) {
        notifier.cancelMeal(mealId)
        mutate { state, now -> ScheduleEngine.snooze(state, mealId, minutes, now) }
    }

    fun setTime(mealId: Int, time: LocalTime) = mutate { state, now ->
        val meal = state.meals.firstOrNull { it.id == mealId } ?: return@mutate state
        if (meal.isCompleted) {
            val millis = java.time.Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
                .atTime(time).atZone(zone).toInstant().toEpochMilli()
            val meals = state.meals.map { if (it.id == mealId) it.copy(completedAt = millis) else it }
            state.copy(meals = ScheduleEngine.reschedule(meals, state.settings))
        } else {
            ScheduleEngine.setManualTime(state, mealId, time, now, zone)
        }
    }

    fun addMeal() = mutate { state, _ -> ScheduleEngine.addMeal(state) }

    fun removeMeal(mealId: Int) {
        alarms.cancelMeal(mealId)
        notifier.cancelMeal(mealId)
        mutate { state, _ -> ScheduleEngine.removeMeal(state, mealId) }
    }

    fun setMealCount(count: Int) = mutate { state, _ -> ScheduleEngine.setMealCount(state, count) }

    fun updateSettings(transform: (Settings) -> Settings) = mutate { state, _ ->
        ScheduleEngine.updateSettings(state, transform(state.settings))
    }

    fun addWater(ounces: Int) = mutate { state, _ ->
        state.copy(waterOz = (state.waterOz + ounces).coerceIn(0, MAX_WATER_OZ))
    }

    fun resetDay() = mutate { state, now ->
        notifier.cancelAllMealAlerts(state)
        state.copy(
            meals = state.meals.map { com.bithead942.mealreminder.data.MealReminder(id = it.id) },
            waterOz = 0,
            dayEpochDay = java.time.Instant.ofEpochMilli(now).atZone(zone).toLocalDate().toEpochDay()
        )
    }

    private fun mutate(transform: (AppState, Long) -> AppState) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val updated = repository.update(now) { transform(it, now) }
            updated.meals.filter { it.isCompleted || it.isSnoozed(now) }.forEach { notifier.cancelMeal(it.id) }
            alarms.sync(updated, now)
            notifier.updateServiceNotification(updated)
            _now.value = now
        }
    }

    private companion object {
        const val TICK_MILLIS = 15_000L
        const val MAX_WATER_OZ = 512
    }
}
