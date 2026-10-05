package com.claudianapolitano.leyla.core

import java.util.Locale

/**
 * A language Leyla can be displayed in. Port of `Us/Core/AppLanguage.swift`.
 *
 * The list is chosen for world coverage rather than country count: one entry
 * per major language bloc (the Americas, Europe, the ex-Soviet states, MENA,
 * South and South-East Asia, East Asia, and the largest African linguae
 * francae), plus Uzbek because that's home.
 *
 * [code] is what the backend's `/v1/translations/{lang}` expects, so it must
 * stay in step with iOS rather than following Android's own locale spelling.
 */
data class AppLanguage(
    val code: String,
    /**
     * The language's name *in that language* — someone looking for their own
     * language recognises "Русский", not "Russian".
     */
    val endonym: String,
    /** The same name in English, shown underneath as a subtitle. */
    val englishName: String,
    /** Written right-to-left; the UI mirrors for these. */
    val isRtl: Boolean = false,
) {
    /** For date and number formatting, so those follow the choice too. */
    val locale: Locale get() = Locale.forLanguageTag(code)

    companion object {
        /**
         * Every language the app offers, roughly ordered by number of speakers
         * so the most likely picks are near the top.
         */
        val all: List<AppLanguage> = listOf(
            AppLanguage("en", "English", "English"),
            AppLanguage("zh-Hans", "简体中文", "Chinese (Simplified)"),
            AppLanguage("es", "Español", "Spanish"),
            AppLanguage("hi", "हिन्दी", "Hindi"),
            AppLanguage("ar", "العربية", "Arabic", isRtl = true),
            AppLanguage("pt-BR", "Português", "Portuguese (Brazil)"),
            AppLanguage("ru", "Русский", "Russian"),
            AppLanguage("bn", "বাংলা", "Bengali"),
            AppLanguage("ja", "日本語", "Japanese"),
            AppLanguage("de", "Deutsch", "German"),
            AppLanguage("fr", "Français", "French"),
            AppLanguage("ko", "한국어", "Korean"),
            AppLanguage("tr", "Türkçe", "Turkish"),
            AppLanguage("vi", "Tiếng Việt", "Vietnamese"),
            AppLanguage("it", "Italiano", "Italian"),
            AppLanguage("id", "Bahasa Indonesia", "Indonesian"),
            AppLanguage("ur", "اردو", "Urdu", isRtl = true),
            AppLanguage("fa", "فارسی", "Persian", isRtl = true),
            AppLanguage("pl", "Polski", "Polish"),
            AppLanguage("uk", "Українська", "Ukrainian"),
            AppLanguage("th", "ไทย", "Thai"),
            AppLanguage("nl", "Nederlands", "Dutch"),
            AppLanguage("sw", "Kiswahili", "Swahili"),
            AppLanguage("fil", "Filipino", "Filipino"),
            AppLanguage("uz", "Oʻzbekcha", "Uzbek"),
            AppLanguage("da", "Dansk", "Danish"),
        )

        fun named(code: String?): AppLanguage? = all.firstOrNull { it.code == code }

        /**
         * The best match for the language the phone itself is set to, so
         * someone who has never opened this screen still gets their own
         * language.
         */
        fun deviceDefault(preferred: List<Locale> = defaultPreferred()): AppLanguage {
            for (locale in preferred) {
                val tag = locale.toLanguageTag()
                all.firstOrNull { it.code.equals(tag, ignoreCase = true) }?.let { return it }
                // "pt-PT" → "pt-BR", "en-GB" → "en": match on the base language.
                val base = tag.substringBefore('-')
                all.firstOrNull { it.code.substringBefore('-') == base }?.let { return it }
            }
            return all.first()
        }

        private fun defaultPreferred(): List<Locale> = listOf(Locale.getDefault())
    }
}
