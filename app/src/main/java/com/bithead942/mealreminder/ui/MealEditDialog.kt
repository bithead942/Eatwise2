package com.bithead942.mealreminder.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.bithead942.mealreminder.data.MealReminder
import java.time.LocalTime

/** Adjusts a single meal's time (or removes the slot). The time picker is shown directly. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MealEditDialog(
    meal: MealReminder,
    position: Int,
    canRemove: Boolean,
    onDismiss: () -> Unit,
    onSetTime: (LocalTime) -> Unit,
    onRemove: () -> Unit
) {
    val initial = localTimeOf(meal.displayTime)
    val pickerState = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = false
    )
    var keyboardInput by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (meal.isCompleted) "Meal $position — eaten at" else "Meal $position — eat at") },
        text = {
            Column {
                if (keyboardInput) TimeInput(state = pickerState) else TimePicker(state = pickerState)
                TextButton(onClick = { keyboardInput = !keyboardInput }) {
                    Text(if (keyboardInput) "Use clock" else "Type a time")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSetTime(LocalTime.of(pickerState.hour, pickerState.minute)) }) {
                Text("Set")
            }
        },
        dismissButton = {
            if (canRemove) {
                TextButton(onClick = onRemove) { Text("Remove") }
            } else {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}
