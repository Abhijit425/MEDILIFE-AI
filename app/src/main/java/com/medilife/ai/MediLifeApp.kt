package com.medilife.ai

import android.app.Application
import com.medilife.ai.reminders.Notifications

class MediLifeApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifications.createChannels(this)
    }
}
