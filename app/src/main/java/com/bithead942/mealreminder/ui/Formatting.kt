package com.bithead942.mealreminder.ui

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

const val BLANK_TIME = "--:-- --"

private fun timeFormatter(): DateTimeFormatter =
    DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())

fun formatTime(millis: Long?): String =
    millis?.let { timeFormatter().format(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault())) }
        ?: BLANK_TIME

fun localTimeOf(millis: Long?): LocalTime =
    millis?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalTime() } ?: LocalTime.now()

fun formatInterval(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return when {
        hours > 0 && mins > 0 -> "${hours}h ${mins}m"
        hours > 0 -> "${hours}h"
        else -> "${mins}m"
    }
}
