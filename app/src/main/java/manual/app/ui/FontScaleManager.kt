package manual.app.ui

import android.app.Activity
import android.content.SharedPreferences
import android.content.res.Configuration
import androidx.core.content.edit


class FontScaleManager(private val preferences: SharedPreferences) {

    var fontScale: Float
        set(value) = preferences.edit { putFloat(KEY_FONT_SCALE, value) }
        get() = preferences.getFloat(KEY_FONT_SCALE, 1f)

    fun attachFontScale(activity: Activity) {
        val configuration = Configuration(activity.resources.configuration)
        configuration.fontScale = fontScale
        activity.resources.updateConfiguration(configuration, activity.resources.displayMetrics)
    }

    companion object {
        const val KEY_FONT_SCALE = "font_scale"
    }
}