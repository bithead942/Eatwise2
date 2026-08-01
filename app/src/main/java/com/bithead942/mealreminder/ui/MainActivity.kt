package com.bithead942.mealreminder.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings as AndroidSettings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bithead942.mealreminder.alarm.ReminderService
import com.bithead942.mealreminder.ui.theme.MealReminderTheme

class MainActivity : ComponentActivity() {

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermission()
        requestExactAlarmPermission()
        ReminderService.start(this)
        setContent {
            MealReminderTheme(darkTheme = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White
                ) {
                    AppRoot(modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing))
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    /** Exact alarms are pre-granted for alarm apps, but some OEM builds still require the toggle. */
    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val alarmManager = getSystemService(android.app.AlarmManager::class.java)
        if (alarmManager.canScheduleExactAlarms()) return
        runCatching {
            startActivity(
                Intent(AndroidSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    .setData(android.net.Uri.parse("package:$packageName"))
            )
        }
    }
}

@Composable
private fun AppRoot(modifier: Modifier = Modifier) {
    val viewModel: MealViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    val now by viewModel.now.collectAsState()
    var showSettings by rememberSaveable { mutableStateOf(false) }

    Surface(modifier = modifier.fillMaxSize(), color = Color.White) {
        if (showSettings) {
            SettingsScreen(
                state = state,
                onBack = { showSettings = false },
                onSettingsChange = { transform -> viewModel.updateSettings(transform) },
                onMealCountChange = viewModel::setMealCount,
                onResetDay = viewModel::resetDay
            )
        } else {
            HomeScreen(
                state = state,
                now = now,
                onToggleMeal = viewModel::toggleMeal,
                onSnooze = viewModel::snooze,
                onSetTime = viewModel::setTime,
                onAddMeal = viewModel::addMeal,
                onRemoveMeal = viewModel::removeMeal,
                onOpenSettings = { showSettings = true }
            )
        }
    }
}
