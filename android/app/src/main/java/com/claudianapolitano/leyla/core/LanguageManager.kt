package com.claudianapolitano.leyla.core

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Owns the app's display language. Port of `Us/Core/LanguageManager.swift`.
 *
 * Android normally picks the language from the system settings. This lets
 * someone choose a different one *inside* Leyla — useful when the phone is in a
 * language they don't share with their partner, or when the phone language
 * isn't one we support but a second language they speak is.
 *
 * The choice takes effect immediately: [Translations] publishes a new table and
 * every string in the app is read through [leylaString], so the screen you are
 * on re-renders in the new language. Nothing needs a restart.
 */
object LanguageManager {

    private const val FILE = "leyla.language"
    private const val KEY_LANGUAGE = "appLanguage"

    private lateinit var prefs: SharedPreferences

    private val _current = MutableStateFlow(AppLanguage.all.first())
    val current: StateFlow<AppLanguage> = _current.asStateFlow()

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        Translations.init(context)
        _current.value = AppLanguage.named(prefs.getString(KEY_LANGUAGE, null))
            ?: AppLanguage.deviceDefault()
        // Instant on relaunch: show whatever was cached last time before the
        // network even gets a chance to answer.
        Translations.loadCachedIfAvailable(_current.value.code)
    }

    /** Fetches the current language's table. Called once the app is running. */
    suspend fun refresh() = Translations.load(_current.value.code)

    /**
     * Switches the display language. The new table is fetched *before* the
     * choice is published, so the re-render never lands on stale text and then
     * jumps a moment later.
     */
    suspend fun select(language: AppLanguage) {
        if (language == _current.value) return
        Translations.load(language.code)
        prefs.edit().putString(KEY_LANGUAGE, language.code).apply()
        _current.value = language
    }
}

/**
 * The app's own `stringResource`.
 *
 * `strings.xml` holds the English text, which is also the key the shared
 * translation table is built on, so this reads the English value and hands it
 * to [Translations]. Falling back to English is the correct behaviour for a
 * missing key, not an error.
 *
 * Subscribing to the table here is what makes a language switch immediate:
 * every string in the app is a reader, so publishing a new table re-renders
 * all of them.
 */
@Composable
fun leylaString(@StringRes id: Int): String {
    val table by Translations.strings.collectAsStateWithLifecycle()
    val english = stringResource(id)
    return table[english] ?: english
}

/** Same, for a string with arguments. */
@Composable
fun leylaString(@StringRes id: Int, vararg formatArgs: Any): String {
    val table by Translations.strings.collectAsStateWithLifecycle()
    val locale = currentLocale()
    // Look the template up by its English form, spelled the way the shared
    // table spells arguments, then bring the answer back to Android's syntax
    // so String.format can fill it in.
    val englishTemplate = stringResource(id)
    val key = Placeholders.toIos(englishTemplate)
    val translated = table[key]?.let(Placeholders::toAndroid) ?: englishTemplate
    return runCatching { String.format(locale, translated, *formatArgs) }
        // A translation with the wrong number of placeholders must not crash
        // the screen it is on; English still reads correctly.
        .getOrElse { String.format(locale, englishTemplate, *formatArgs) }
}

/** Translates a literal that has no `strings.xml` entry. */
@Composable
fun tr(english: String): String {
    val table by Translations.strings.collectAsStateWithLifecycle()
    return table[english] ?: english
}

/** The locale that dates, numbers and plurals should follow. */
@Composable
fun currentLocale(): Locale {
    val language by LanguageManager.current.collectAsStateWithLifecycle()
    return language.locale
}
