# Eatwise2

An Android meal reminder app in the style of Eatwise: a day of evenly spaced meal slots that
re-space themselves around when you actually eat.

## Features

- **Variable number of meals** — 6 by default; add one with *+ Extra Reminder* or remove one from a
  meal's edit dialog. The count is also adjustable in Settings.
- **Blank until the day starts** — no times are shown until the first meal of the day is marked as
  eaten; the rest of the day is then laid out from that moment.
- **2.5 hour spacing, adjustable** — the gap between meals is configurable in 15 minute steps.
- **Self-correcting schedule** — marking a 12:00 PM meal as eaten at 12:05 PM moves the next meal to
  2:35 PM and shifts every later meal by the same amount.
- **Repeating alerts** — sound and/or vibration at the meal time, repeated every 5 minutes until the
  meal is marked as eaten.
- **Any system sound** — pick any ringtone, notification or alarm tone through the system picker;
  the choice is remembered.
- **Snooze 15 / 20 / 30 minutes** — from the notification or from the meal's edit dialog.
- **Runs in the background** — a foreground service keeps the app resident and exact alarms fire
  even when the UI is closed; alarms are re-armed after a reboot.

## Build

```bash
./gradlew assembleDebug          # APK at app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest      # scheduling rules
./gradlew lintDebug
```

Requires JDK 17 and the Android SDK (compileSdk 34, minSdk 26).

## Layout

| Path | Contents |
| --- | --- |
| `data/` | Serializable state (`MealReminder`, `Settings`, `AppState`) and the DataStore repository |
| `domain/ScheduleEngine.kt` | Pure scheduling rules: spacing, completion, snooze, day rollover |
| `alarm/` | Exact alarm scheduling, alert notifications, boot receiver, resident foreground service |
| `ui/` | Compose screens: meal list, settings, meal edit dialog |
