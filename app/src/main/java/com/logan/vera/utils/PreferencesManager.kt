package com.logan.vera.utils

import android.content.Context
import androidx.compose.ui.text.font.FontFamily
import com.logan.vera.ui.theme.Fonts
import com.logan.vera.ui.theme.ReaderTheme
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("reader_preferences", Context.MODE_PRIVATE)

    var isDarkMode: Boolean
        get() = prefs.getBoolean("dark_mode", false)
        set(value) = prefs.edit().putBoolean("dark_mode", value).apply()

    var fontSize: Float
        get() = prefs.getFloat("font_size", 16f)
        set(value) = prefs.edit().putFloat("font_size", value).apply()

    var fontFamily: FontFamily
        get() = when (prefs.getString("font_family", "Roboto")) {
            "Arimo" -> Fonts.Arimo
            "Garamond" -> Fonts.Garamond
            "Lora" -> Fonts.Lora
            "Merriweather" -> Fonts.Merriweather
            "NotoSerif" -> Fonts.NotoSerif
            "OpenSans" -> Fonts.OpenSans
            else -> Fonts.Roboto
        }
        set(value) = prefs.edit().putString("font_family", when (value) {
            Fonts.Arimo -> "Arimo"
            Fonts.Garamond -> "Garamond"
            Fonts.Lora -> "Lora"
            Fonts.Merriweather -> "Merriweather"
            Fonts.NotoSerif -> "NotoSerif"
            Fonts.OpenSans -> "OpenSans"
            else -> "Roboto"
        }).apply()

    var theme: ReaderTheme
        get() = ReaderTheme.valueOf(
            prefs.getString("theme", ReaderTheme.LIGHT.name) ?: ReaderTheme.LIGHT.name
        )
        set(value) = prefs.edit().putString("theme", value.name).apply()
}
