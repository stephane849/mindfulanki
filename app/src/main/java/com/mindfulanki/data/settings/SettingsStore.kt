package com.mindfulanki.data.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Persists [AppSettings] in SharedPreferences and exposes them as a [StateFlow]
 * so the UI re-renders when a setting changes (e.g. invert mode flips the theme).
 */
class SettingsStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("mindfulanki_settings", Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<AppSettings> = _settings

    fun update(settings: AppSettings) {
        prefs.edit()
            .putBoolean(KEY_INVERT, settings.invert)
            .putInt(KEY_NEW_PER_DAY, settings.newPerDay)
            .putInt(KEY_DAILY_GOAL, settings.dailyGoal)
            .putBoolean(KEY_SHOW_INTERVALS, settings.showIntervals)
            .putString(KEY_VARIANT, settings.variant.name)
            .apply()
        _settings.value = settings
    }

    private fun load(): AppSettings {
        val defaults = AppSettings()
        return AppSettings(
            invert = prefs.getBoolean(KEY_INVERT, defaults.invert),
            newPerDay = prefs.getInt(KEY_NEW_PER_DAY, defaults.newPerDay),
            dailyGoal = prefs.getInt(KEY_DAILY_GOAL, defaults.dailyGoal),
            showIntervals = prefs.getBoolean(KEY_SHOW_INTERVALS, defaults.showIntervals),
            variant = runCatching { StudyVariant.valueOf(prefs.getString(KEY_VARIANT, defaults.variant.name)!!) }
                .getOrDefault(defaults.variant),
        )
    }

    private companion object {
        const val KEY_INVERT = "invert"
        const val KEY_NEW_PER_DAY = "new_per_day"
        const val KEY_DAILY_GOAL = "daily_goal"
        const val KEY_SHOW_INTERVALS = "show_intervals"
        const val KEY_VARIANT = "variant"
    }
}
