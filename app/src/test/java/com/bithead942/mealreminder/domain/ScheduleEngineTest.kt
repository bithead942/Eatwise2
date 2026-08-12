package com.bithead942.mealreminder.domain

import com.bithead942.mealreminder.data.AppState
import com.bithead942.mealreminder.data.Settings
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleEngineTest {

    private val zone: ZoneId = ZoneId.of("UTC")
    private val today: LocalDate = LocalDate.of(2026, 8, 1)

    private fun at(hour: Int, minute: Int): Long =
        today.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()

    private fun state(mealCount: Int = 6, settings: Settings = Settings()) =
        AppState(meals = AppState.defaultMeals(mealCount), settings = settings, dayEpochDay = today.toEpochDay())

    @Test
    fun `schedule starts blank`() {
        val scheduled = ScheduleEngine.reschedule(state().meals, Settings())
        assertTrue(scheduled.all { it.scheduledAt == null })
    }

    @Test
    fun `first completed meal seeds the rest at the configured interval`() {
        val result = ScheduleEngine.markCompleted(state(), mealId = 1, now = at(7, 0))

        assertEquals(at(7, 0), result.meals[0].completedAt)
        assertEquals(at(9, 30), result.meals[1].scheduledAt)
        assertEquals(at(12, 0), result.meals[2].scheduledAt)
        assertEquals(at(14, 30), result.meals[3].scheduledAt)
        assertEquals(at(17, 0), result.meals[4].scheduledAt)
        assertEquals(at(19, 30), result.meals[5].scheduledAt)
    }

    @Test
    fun `completing a meal late shifts every following meal`() {
        var result = ScheduleEngine.markCompleted(state(), mealId = 1, now = at(7, 0))
        assertEquals(at(9, 30), result.meals[1].scheduledAt)

        // Meal 2 was due at 9:30 but was eaten at 9:35.
        result = ScheduleEngine.markCompleted(result, mealId = 2, now = at(9, 35))

        assertEquals(at(12, 5), result.meals[2].scheduledAt)
        assertEquals(at(14, 35), result.meals[3].scheduledAt)
        assertEquals(at(17, 5), result.meals[4].scheduledAt)
        assertEquals(at(19, 35), result.meals[5].scheduledAt)
    }

    @Test
    fun `custom interval is honoured`() {
        val custom = Settings(intervalMinutes = 180)
        val result = ScheduleEngine.markCompleted(state(settings = custom), mealId = 1, now = at(8, 0))

        assertEquals(at(11, 0), result.meals[1].scheduledAt)
        assertEquals(at(14, 0), result.meals[2].scheduledAt)
    }

    @Test
    fun `manual time pins a meal and spaces the following meals from it`() {
        val result = ScheduleEngine.setManualTime(
            state(),
            mealId = 2,
            timeOfDay = LocalTime.of(10, 0),
            now = at(6, 0),
            zone = zone
        )

        assertNull(result.meals[0].scheduledAt)
        assertEquals(at(10, 0), result.meals[1].scheduledAt)
        assertEquals(at(12, 30), result.meals[2].scheduledAt)
    }

    @Test
    fun `un-completing a meal reverts the schedule`() {
        var result = ScheduleEngine.markCompleted(state(), mealId = 1, now = at(7, 0))
        result = ScheduleEngine.markPending(result, mealId = 1)

        assertTrue(result.meals.all { it.scheduledAt == null && it.completedAt == null })
    }

    @Test
    fun `adding and removing reminders keeps the spacing`() {
        var result = ScheduleEngine.markCompleted(state(), mealId = 1, now = at(7, 0))
        result = ScheduleEngine.addMeal(result)

        assertEquals(7, result.meals.size)
        assertEquals(at(22, 0), result.meals[6].scheduledAt)

        result = ScheduleEngine.removeMeal(result, mealId = 3)
        assertEquals(6, result.meals.size)
        assertEquals(at(12, 0), result.meals[2].scheduledAt)
    }

    @Test
    fun `meal count can be changed from the default of six`() {
        assertEquals(6, AppState().meals.size)
        assertEquals(8, ScheduleEngine.setMealCount(state(), 8).meals.size)
        assertEquals(3, ScheduleEngine.setMealCount(state(), 3).meals.size)
    }

    @Test
    fun `overdue meals repeat every five minutes until completed`() {
        val settings = Settings()
        val result = ScheduleEngine.markCompleted(state(settings = settings), mealId = 1, now = at(7, 0))
        val meal = result.meals[1]

        assertEquals(at(9, 30), ScheduleEngine.nextAlertAt(meal, settings, at(9, 0)))
        assertEquals(at(9, 35), ScheduleEngine.nextAlertAt(meal, settings, at(9, 31)))
        assertEquals(at(9, 40), ScheduleEngine.nextAlertAt(meal, settings, at(9, 37)))
    }

    @Test
    fun `completed meals stop alerting`() {
        val result = ScheduleEngine.markCompleted(state(), mealId = 1, now = at(7, 0))
        assertNull(ScheduleEngine.nextAlertAt(result.meals[0], Settings(), at(7, 30)))
    }

    @Test
    fun `snooze suppresses alerts until it expires`() {
        var result = ScheduleEngine.markCompleted(state(), mealId = 1, now = at(7, 0))
        result = ScheduleEngine.snooze(result, mealId = 2, minutes = 20, now = at(9, 31))
        val meal = result.meals[1]

        assertEquals(at(9, 51), ScheduleEngine.nextAlertAt(meal, Settings(), at(9, 32)))
        assertEquals(at(9, 55), ScheduleEngine.nextAlertAt(meal, Settings(), at(9, 52)))
    }

    @Test
    fun `a new day blanks the schedule`() {
        val result = ScheduleEngine.markCompleted(state(), mealId = 1, now = at(7, 0))
        val tomorrow = today.plusDays(1).atTime(6, 0).atZone(zone).toInstant().toEpochMilli()

        val rolled = ScheduleEngine.rollOverDay(result, tomorrow, zone)

        assertTrue(rolled.meals.all { it.completedAt == null && it.scheduledAt == null })
        assertEquals(today.plusDays(1).toEpochDay(), rolled.dayEpochDay)
    }
}
