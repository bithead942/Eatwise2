package com.bithead942.mealreminder.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.bithead942.mealreminder.R
import com.bithead942.mealreminder.data.AppState
import com.bithead942.mealreminder.data.MealReminder
import com.bithead942.mealreminder.data.SNOOZE_15
import com.bithead942.mealreminder.data.SNOOZE_20
import com.bithead942.mealreminder.data.SNOOZE_30
import com.bithead942.mealreminder.data.Settings
import com.bithead942.mealreminder.domain.ScheduleEngine
import com.bithead942.mealreminder.ui.MainActivity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Builds the meal alert notifications and the ongoing "app is armed" notification. */
class ReminderNotifier(private val context: Context) {

    private val manager: NotificationManager = context.getSystemService(NotificationManager::class.java)
    private val timeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())

    fun createServiceChannel() {
        val channel = NotificationChannel(
            SERVICE_CHANNEL_ID,
            context.getString(R.string.notification_channel_service),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            setShowBadge(false)
            description = "Shows the next scheduled meal while reminders are armed."
        }
        manager.createNotificationChannel(channel)
    }

    /**
     * The alert channel is intentionally silent with no vibration: the sound and vibration are
     * played manually exactly once per fire (see [playAlertOnce]) so they never loop. Older
     * sound-bearing channels from previous versions are removed.
     */
    private fun alertChannelId(): String {
        val id = ALERT_CHANNEL_ID
        if (manager.getNotificationChannel(id) == null) {
            val channel = NotificationChannel(
                id,
                context.getString(R.string.notification_channel_alerts),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setSound(null, null)
                enableVibration(false)
                setBypassDnd(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(channel)
        }
        manager.notificationChannels
            .filter { it.id.startsWith("meal_alerts_") && it.id != id }
            .forEach { manager.deleteNotificationChannel(it.id) }
        return id
    }

    /** Plays the chosen sound once and vibrates once; nothing loops. */
    private fun playAlertOnce(settings: Settings) {
        if (settings.soundEnabled) {
            runCatching {
                val player = MediaPlayer()
                player.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build()
                )
                player.setDataSource(context, resolveSound(settings))
                player.isLooping = false
                player.setOnCompletionListener { it.release() }
                player.setOnErrorListener { mp, _, _ -> mp.release(); true }
                player.prepare()
                player.start()
            }
        }
        if (settings.vibrateEnabled) {
            runCatching {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getSystemService(VibratorManager::class.java).defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Vibrator::class.java)
                }
                // repeat index -1 means play the pattern a single time.
                vibrator.vibrate(VibrationEffect.createWaveform(VIBRATION_PATTERN, -1))
            }
        }
    }

    fun resolveSound(settings: Settings): Uri =
        settings.soundUri?.let(Uri::parse)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    fun notifyMeal(meal: MealReminder, position: Int, settings: Settings, now: Long = System.currentTimeMillis()) {
        val channelId = alertChannelId()
        val scheduled = meal.scheduledAt?.let { format(it) }
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Time to eat — meal $position")
            .setContentText(
                if (scheduled != null) "Scheduled for $scheduled. Tap to open, or snooze."
                else "Tap to open, or snooze."
            )
            .setWhen(now)
            .setShowWhen(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(false)
            .setOngoing(false)
            .setContentIntent(contentIntent())
            .setFullScreenIntent(contentIntent(), true)
            .addAction(0, "SNOOZE 15", actionIntent(NotificationActionReceiver.ACTION_SNOOZE, meal.id, SNOOZE_15))
            .addAction(0, "SNOOZE 20", actionIntent(NotificationActionReceiver.ACTION_SNOOZE, meal.id, SNOOZE_20))
            .addAction(0, "SNOOZE 30", actionIntent(NotificationActionReceiver.ACTION_SNOOZE, meal.id, SNOOZE_30))
            .build()
        manager.notify(alertNotificationId(meal.id), notification)
        playAlertOnce(settings)
    }

    fun cancelMeal(mealId: Int) = manager.cancel(alertNotificationId(mealId))

    fun cancelAllMealAlerts(state: AppState) = state.meals.forEach { cancelMeal(it.id) }

    fun buildServiceNotification(state: AppState?, now: Long = System.currentTimeMillis()): Notification {
        val next = state?.let { ScheduleEngine.nextMeal(it, now) }
        val text = when {
            state == null -> "Reminders are armed."
            next == null -> "All meals are done for today."
            next.scheduledAt == null -> "Mark your first meal to start today's schedule."
            else -> "Next meal at ${format(maxOf(next.scheduledAt, next.snoozedUntil ?: 0L))}"
        }
        return NotificationCompat.Builder(context, SERVICE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(text)
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(contentIntent())
            .build()
    }

    fun updateServiceNotification(state: AppState) {
        manager.notify(SERVICE_NOTIFICATION_ID, buildServiceNotification(state))
    }

    private fun format(millis: Long): String =
        timeFormatter.format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))

    private fun contentIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun actionIntent(action: String, mealId: Int, minutes: Int): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            this.action = action
            putExtra(AlarmReceiver.EXTRA_MEAL_ID, mealId)
            putExtra(NotificationActionReceiver.EXTRA_SNOOZE_MINUTES, minutes)
        }
        val requestCode = mealId * 100 + minutes + action.hashCode() % 7
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun alertNotificationId(mealId: Int) = ALERT_NOTIFICATION_BASE + mealId

    companion object {
        const val SERVICE_CHANNEL_ID = "meal_reminder_service"
        private const val ALERT_CHANNEL_ID = "meal_alerts_silent"
        const val SERVICE_NOTIFICATION_ID = 1
        private const val ALERT_NOTIFICATION_BASE = 1000
        private val VIBRATION_PATTERN = longArrayOf(0, 500, 300, 500, 300, 700)
    }
}
