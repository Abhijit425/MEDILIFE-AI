package com.medilife.ai

import android.content.Context
import com.medilife.ai.data.AppDatabase
import com.medilife.ai.data.HealthRepository
import com.medilife.ai.data.SettingsStore
import com.medilife.ai.engine.LocalLlm
import com.medilife.ai.engine.MediAiEngine
import com.medilife.ai.reminders.ReminderScheduler

/** Simple manual dependency container (no network, no analytics). */
class AppContainer(context: Context) {
    val app: Context = context.applicationContext
    val db: AppDatabase = AppDatabase.build(app)
    val repo = HealthRepository(db)
    val settings = SettingsStore(app)
    val llm = LocalLlm(app)
    val ai = MediAiEngine(llm)
    val reminders = ReminderScheduler(app)
}
