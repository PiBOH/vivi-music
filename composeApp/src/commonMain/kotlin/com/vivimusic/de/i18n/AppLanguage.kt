package com.vivimusic.de.i18n

/**
 * The languages the application supports. [code] is a BCP-47 language tag used
 * both for the Android resource qualifiers (e.g. `values-zh-rCN`) and for the
 * platform locale that Compose Multiplatform resolves string resources with.
 *
 * To add a new language:
 * 1. add an entry to this list;
 * 2. create `composeApp/src/commonMain/composeResources/values-XX/strings.xml`
 *    (replacing `XX` with the qualifier) and translate the keys from the
 *    default `values/strings.xml` file.
 */
data class AppLanguage(
    val code: String,
    val nativeName: String,
)

val supportedLanguages: List<AppLanguage> = listOf(
    AppLanguage("en", "English"),
    AppLanguage("az", "Azərbaycan dili"),
    AppLanguage("bs", "Bosanski"),
    AppLanguage("ca", "Català"),
    AppLanguage("cs", "Čeština"),
    AppLanguage("de", "Deutsch"),
    AppLanguage("et", "Eesti"),
    AppLanguage("es", "Español"),
    AppLanguage("eu", "Euskara"),
    AppLanguage("fil", "Filipino"),
    AppLanguage("fr", "Français"),
    AppLanguage("hr", "Hrvatski"),
    AppLanguage("id", "Bahasa Indonesia"),
    AppLanguage("it", "Italiano"),
    AppLanguage("lt", "Lietuvių"),
    AppLanguage("hu", "Magyar"),
    AppLanguage("ms", "Bahasa Melayu"),
    AppLanguage("nl", "Nederlands"),
    AppLanguage("nb", "Norsk bokmål"),
    AppLanguage("pl", "Polski"),
    AppLanguage("pt", "Português"),
    AppLanguage("ro", "Română"),
    AppLanguage("sk", "Slovenčina"),
    AppLanguage("sl", "Slovenščina"),
    AppLanguage("sr", "Српски"),
    AppLanguage("fi", "Suomi"),
    AppLanguage("sv", "Svenska"),
    AppLanguage("vi", "Tiếng Việt"),
    AppLanguage("tr", "Türkçe"),
    AppLanguage("el", "Ελληνικά"),
    AppLanguage("be", "Беларуская"),
    AppLanguage("bg", "Български"),
    AppLanguage("ru", "Русский"),
    AppLanguage("uk", "Українська"),
    AppLanguage("he", "עברית"),
    AppLanguage("ar", "العربية"),
    AppLanguage("hi", "हिन्दी"),
    AppLanguage("as", "অসমীয়া"),
    AppLanguage("bn", "বাংলা"),
    AppLanguage("pa", "ਪੰਜਾਬੀ"),
    AppLanguage("ta", "தமிழ்"),
    AppLanguage("te", "తెలుగు"),
    AppLanguage("ml", "മലയാളം"),
    AppLanguage("th", "ไทย"),
    AppLanguage("km", "ខ្មែរ"),
    AppLanguage("ko", "한국어"),
    AppLanguage("zh-CN", "简体中文"),
    AppLanguage("zh-TW", "繁體中文"),
    AppLanguage("ja", "日本語"),
)
