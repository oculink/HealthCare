package com.fyp.healthcare.i18n

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

/**
 * The two languages CareApp ships with.
 *
 * [tag] is the BCP-47 tag handed to [Locale], [shortLabel] is what the top-right switcher
 * prints ("EN" / "BM"), and [displayName] is what the picker lists.
 */
enum class AppLanguage(
    val tag: String,
    val shortLabel: String,
    val displayName: String,
) {
    ENGLISH("en", "EN", "English"),
    MALAY("ms", "BM", "Bahasa Melayu"),
    ;

    val locale: Locale get() = Locale.forLanguageTag(tag)
}

/**
 * App-wide language choice.
 *
 * English is the source language: every screen is written in English and [LocalizedText]
 * looks the string up in [MalayStrings] when Malay is selected. A string that hasn't been
 * translated yet simply stays in English, so the app never shows a missing-text key.
 *
 * The choice is stored on this device only (SharedPreferences "app_prefs"), never synced to
 * the account - two people sharing one login can each keep their own language.
 */
object Language {

    var current by mutableStateOf(AppLanguage.ENGLISH)
        private set

    /** Load the saved choice - call once from MainActivity.onCreate. */
    fun init(context: Context) {
        val stored = prefs(context).getString(KEY, null)
        current = AppLanguage.entries.firstOrNull { it.name == stored } ?: AppLanguage.ENGLISH
        applyLocale(current)
    }

    fun set(context: Context, language: AppLanguage) {
        if (current == language) {
            // Still persist: the first run has no stored value, and picking English should
            // record that so a later OS default can't flip the app.
            prefs(context).edit().putString(KEY, language.name).apply()
            applyLocale(language)
            return
        }
        current = language
        prefs(context).edit().putString(KEY, language.name).apply()
        applyLocale(language)
    }

    /** Flip between the two languages - the chip's one-tap path. */
    fun toggle(context: Context) {
        set(context, if (current == AppLanguage.ENGLISH) AppLanguage.MALAY else AppLanguage.ENGLISH)
    }

    /**
     * Dates, numbers and month names all go through Locale.getDefault() in the screens, so
     * pointing the default at the chosen language is what turns "12 Mar 1958" into
     * "12 Mac 1958" without touching every formatter.
     */
    private fun applyLocale(language: AppLanguage) {
        Locale.setDefault(language.locale)
    }

    private fun prefs(c: Context) = c.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    private const val KEY = "app_language"
}
