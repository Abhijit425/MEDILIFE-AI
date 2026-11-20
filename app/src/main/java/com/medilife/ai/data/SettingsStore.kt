package com.medilife.ai.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "medilife_settings")

data class AppSettings(
    val themeMode: String = "DARK",
    val biometricLock: Boolean = false,
    val remindersEnabled: Boolean = true,
    val hydrationReminders: Boolean = false,
    val onboardingDone: Boolean = false,
    val llmEnabled: Boolean = false,
    val llmModelPath: String = "",
    val emergencyNumber: String = "112",
    val reduceMotion: Boolean = false
)

class SettingsStore(private val context: Context) {
    private object K {
        val theme = stringPreferencesKey("theme")
        val bio = booleanPreferencesKey("bio")
        val rem = booleanPreferencesKey("reminders")
        val hyd = booleanPreferencesKey("hydration")
        val onb = booleanPreferencesKey("onboarding")
        val llm = booleanPreferencesKey("llm")
        val llmPath = stringPreferencesKey("llm_path")
        val emerg = stringPreferencesKey("emergency_number")
        val motion = booleanPreferencesKey("reduce_motion")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            themeMode = p[K.theme] ?: "DARK",
            biometricLock = p[K.bio] ?: false,
            remindersEnabled = p[K.rem] ?: true,
            hydrationReminders = p[K.hyd] ?: false,
            onboardingDone = p[K.onb] ?: false,
            llmEnabled = p[K.llm] ?: false,
            llmModelPath = p[K.llmPath] ?: "",
            emergencyNumber = p[K.emerg] ?: "112",
            reduceMotion = p[K.motion] ?: false
        )
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setTheme(v: String) = context.dataStore.edit { it[K.theme] = v }
    suspend fun setBiometric(v: Boolean) = context.dataStore.edit { it[K.bio] = v }
    suspend fun setReminders(v: Boolean) = context.dataStore.edit { it[K.rem] = v }
    suspend fun setHydration(v: Boolean) = context.dataStore.edit { it[K.hyd] = v }
    suspend fun setOnboardingDone(v: Boolean) = context.dataStore.edit { it[K.onb] = v }
    suspend fun setLlmEnabled(v: Boolean) = context.dataStore.edit { it[K.llm] = v }
    suspend fun setLlmPath(v: String) = context.dataStore.edit { it[K.llmPath] = v }
    suspend fun setEmergencyNumber(v: String) = context.dataStore.edit { it[K.emerg] = v }
    suspend fun setReduceMotion(v: Boolean) = context.dataStore.edit { it[K.motion] = v }
    suspend fun clear() = context.dataStore.edit { it.clear() }
}
