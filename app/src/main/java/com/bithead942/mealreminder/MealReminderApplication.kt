package com.bithead942.mealreminder

import android.app.Application
import android.content.Context
import com.bithead942.mealreminder.alarm.AlarmScheduler
import com.bithead942.mealreminder.alarm.ReminderNotifier
import com.bithead942.mealreminder.alarm.ReminderService
import com.bithead942.mealreminder.data.MealRepository

class MealReminderApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.notifier.createServiceChannel()
        ReminderService.start(this)
    }

    companion object {
        lateinit var container: AppContainer
            private set
    }
}

/** Minimal manual dependency graph; the receivers and the service resolve it lazily. */
class AppContainer(context: Context) {
    val repository = MealRepository(context.applicationContext)
    val alarmScheduler = AlarmScheduler(context.applicationContext)
    val notifier = ReminderNotifier(context.applicationContext)
}

/** Safe accessor for components that may be created before [MealReminderApplication.onCreate]. */
fun Context.appContainer(): AppContainer {
    val app = applicationContext as? MealReminderApplication
    return if (app != null) MealReminderApplication.container else AppContainer(applicationContext)
}
