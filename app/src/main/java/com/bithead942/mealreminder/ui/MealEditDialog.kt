package com.bithead942.mealreminder.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bithead942.mealreminder.data.MealReminder
import com.bithead942.mealreminder.data.SNOOZE_OPTIONS
import com.bithead942.mealreminder.ui.theme.TextMuted
import java.time.LocalTime

/** Actions for a single meal: adjust its time, snooze it, toggle it, or remove the slot. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MealEditDialog(
    meal: MealReminder,
    position: Int,
    canRemove: Boolean,
    onDismiss: () -> Unit,
    onSetTime: (LocalTime) -> Unit,
    onSnooze: (Int) -> Unit,
    onToggle: () -> Unit,
    onRemove: () -> Unit
) {
    var showTimePicker by remember { mutableStateOf(false) }

    if (showTimePicker) {
        val initial = localTimeOf(meal.displayTime)
        val pickerState = rememberTimePickerState(
            initialHour = initial.hour,
            initialMinute = initial.minute,
            is24Hour = false
        )
        var keyboardInput by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(if (meal.isCompleted) "Eaten at" else "Eat at") },
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
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Meal $position") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (meal.isCompleted) "Eaten at ${formatTime(meal.completedAt)}"
                    else "Scheduled for ${formatTime(meal.scheduledAt)}",
                    color = TextMuted
                )
                Button(onClick = { showTimePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (meal.isCompleted) "Change eaten time" else "Change scheduled time")
                }
                if (!meal.isCompleted) {
                    Text("Snooze", fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SNOOZE_OPTIONS.forEach { minutes ->
                            OutlinedButton(
                                onClick = { onSnooze(minutes) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("$minutes m")
                            }
                        }
                    }
                }
                OutlinedButton(onClick = onToggle, modifier = Modifier.fillMaxWidth()) {
                    Text(if (meal.isCompleted) "Mark as not eaten" else "Mark as eaten now")
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        dismissButton = {
            if (canRemove) {
                TextButton(onClick = onRemove) { Text("Remove") }
            }
        }
    )
}
