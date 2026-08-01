package com.bithead942.mealreminder.ui

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bithead942.mealreminder.R
import com.bithead942.mealreminder.data.AppState
import com.bithead942.mealreminder.data.Settings
import com.bithead942.mealreminder.ui.theme.Cyan
import com.bithead942.mealreminder.ui.theme.Divider
import com.bithead942.mealreminder.ui.theme.TextMuted
import com.bithead942.mealreminder.ui.theme.TextPrimary

@Composable
fun SettingsScreen(
    state: AppState,
    onBack: () -> Unit,
    onSettingsChange: ((Settings) -> Settings) -> Unit,
    onMealCountChange: (Int) -> Unit,
    onResetDay: () -> Unit
) {
    val context = LocalContext.current
    val settings = state.settings

    val ringtonePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            val name = uri?.let {
                runCatching { RingtoneManager.getRingtone(context, it)?.getTitle(context) }.getOrNull()
            }
            onSettingsChange { it.copy(soundUri = uri?.toString(), soundName = name ?: "Silent") }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = TextPrimary
                )
            }
            Text(stringResource(R.string.settings), fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        }
        HorizontalDivider(color = Divider)

        SectionTitle("Schedule")
        StepperRow(
            label = "Meal reminders",
            value = "${state.meals.size}",
            onDecrease = { onMealCountChange(state.meals.size - 1) },
            onIncrease = { onMealCountChange(state.meals.size + 1) }
        )
        StepperRow(
            label = "Time between meals",
            value = formatInterval(settings.intervalMinutes),
            onDecrease = {
                onSettingsChange { it.copy(intervalMinutes = (it.intervalMinutes - 15).coerceAtLeast(15)) }
            },
            onIncrease = {
                onSettingsChange { it.copy(intervalMinutes = (it.intervalMinutes + 15).coerceAtMost(12 * 60)) }
            }
        )
        StepperRow(
            label = "Repeat alert every",
            value = formatInterval(settings.repeatMinutes),
            onDecrease = {
                onSettingsChange { it.copy(repeatMinutes = (it.repeatMinutes - 1).coerceAtLeast(1)) }
            },
            onIncrease = {
                onSettingsChange { it.copy(repeatMinutes = (it.repeatMinutes + 1).coerceAtMost(60)) }
            }
        )

        SectionTitle("Alerts")
        SwitchRow(
            label = "Play a sound",
            checked = settings.soundEnabled,
            onCheckedChange = { checked -> onSettingsChange { it.copy(soundEnabled = checked) } }
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Reminder sound", color = TextPrimary)
                Text(settings.soundName ?: "Default alarm sound", color = TextMuted, fontSize = 12.sp)
            }
            Button(onClick = {
                val current = settings.soundUri?.let(Uri::parse)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select meal reminder sound")
                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, current)
                }
                ringtonePicker.launch(intent)
            }) {
                Text("Choose")
            }
        }
        SwitchRow(
            label = "Vibrate",
            checked = settings.vibrateEnabled,
            onCheckedChange = { checked -> onSettingsChange { it.copy(vibrateEnabled = checked) } }
        )

        SectionTitle("Today")
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            OutlinedButton(
                onClick = { onResetDay(); onBack() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Clear today's schedule")
            }
        }
        Text(
            text = "The schedule stays blank until the first meal of the day is marked as eaten. " +
                "Every following meal is then spaced by the time between meals, and shifts again " +
                "each time a meal is marked.",
            color = TextMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        color = Cyan,
        fontSize = 12.sp,
        letterSpacing = 1.5.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp)
    )
}

@Composable
private fun StepperRow(label: String, value: String, onDecrease: () -> Unit, onIncrease: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextPrimary, modifier = Modifier.weight(1f))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = onDecrease) { Text("-") }
            Text(value, color = TextPrimary, fontWeight = FontWeight.Medium)
            OutlinedButton(onClick = onIncrease) { Text("+") }
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextPrimary, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
