package com.bithead942.mealreminder.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bithead942.mealreminder.domain.ScheduleEngine
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "meal_reminder")

/** Single source of truth for the schedule; persisted as JSON in DataStore. */
class MealRepository(private val context: Context, private val zone: ZoneId = ZoneId.systemDefault()) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    val state: Flow<AppState> = context.dataStore.data.map { prefs -> prefs.readState() }

    suspend fun current(): AppState = state.first()

    /**
     * Applies [transform] to the persisted state. The day rollover and the spacing rules are always
     * re-applied so callers cannot persist an inconsistent schedule.
     */
    suspend fun update(now: Long = System.currentTimeMillis(), transform: (AppState) -> AppState): AppState {
        var result: AppState = AppState()
        context.dataStore.edit { prefs ->
            val rolled = ScheduleEngine.rollOverDay(prefs.readState(), now, zone)
            val updated = transform(rolled)
            result = updated.copy(meals = ScheduleEngine.reschedule(updated.meals, updated.settings))
            prefs[STATE_KEY] = json.encodeToString(AppState.serializer(), result)
        }
        return result
    }

    private fun Preferences.readState(): AppState {
        val raw = this[STATE_KEY] ?: return AppState(dayEpochDay = today())
        return runCatching { json.decodeFromString(AppState.serializer(), raw) }
            .getOrElse { AppState(dayEpochDay = today()) }
    }

    private fun today(): Long =
        java.time.Instant.now().atZone(zone).toLocalDate().toEpochDay()

    companion object {
        private val STATE_KEY = stringPreferencesKey("app_state")
    }
}
