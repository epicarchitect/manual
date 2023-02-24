package manual.app.ui

import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit

class NightModeManager(private val preferences: SharedPreferences) {

    var mode =
        preferences.getInt(KEY_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM).toThemeMode()
        set(value) {
            field = value
            value.toAppCompatDelegateMode().let {
                preferences.edit { putInt(KEY_MODE, it) }
                AppCompatDelegate.setDefaultNightMode(it)
            }
        }

    init {
        AppCompatDelegate.setDefaultNightMode(mode.toAppCompatDelegateMode())
    }

    private fun Int.toThemeMode() = when (this) {
        AppCompatDelegate.MODE_NIGHT_YES -> Mode.NIGHT
        AppCompatDelegate.MODE_NIGHT_NO -> Mode.NOT_NIGHT
        else -> Mode.FOLLOW_SYSTEM
    }

    private fun Mode.toAppCompatDelegateMode() = when (this) {
        Mode.NIGHT -> AppCompatDelegate.MODE_NIGHT_YES
        Mode.NOT_NIGHT -> AppCompatDelegate.MODE_NIGHT_NO
        Mode.FOLLOW_SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }

    enum class Mode {
        NIGHT,
        NOT_NIGHT,
        FOLLOW_SYSTEM
    }

    companion object {
        private const val KEY_MODE = "mode"
    }
}