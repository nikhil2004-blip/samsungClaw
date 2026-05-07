package com.example.signal.data.repository

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private const val ONBOARDING_PREFERENCES_NAME = "signal_preferences"
private val Context.dataStore by preferencesDataStore(name = ONBOARDING_PREFERENCES_NAME)

@Singleton
class OnboardingRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val onboardingCompletedKey = booleanPreferencesKey("onboarding_completed")
    private val isDarkThemeKey         = booleanPreferencesKey("is_dark_theme")

    val onboardingCompleted: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[onboardingCompletedKey] ?: false }

    /** Defaults to true (dark) — can be toggled by the user in Settings. */
    val isDarkTheme: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[isDarkThemeKey] ?: true }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[onboardingCompletedKey] = completed }
    }

    suspend fun setDarkTheme(dark: Boolean) {
        context.dataStore.edit { it[isDarkThemeKey] = dark }
    }
}
