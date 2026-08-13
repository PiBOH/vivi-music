package com.vivimusic.de.data

/**
 * Runtime configuration, populated by the desktop entry point (`main.kt`).
 *
 * Values are read, in order, from a JVM system property, the process
 * environment, or a `.env` file next to the executable. The InnerTube API key
 * is normally injected at build time by the CI
 * (see `composeApp/build.gradle.kts`).
 *
 * When a value is empty the related feature is disabled: Supabase sync and
 * InnerTube (YouTube Music) both degrade to local-only.
 */
object AppConfig {
    var supabaseUrl: String = ""
    var supabaseAnonKey: String = ""
    var innerTubeApiKey: String = ""

    val isSyncConfigured: Boolean
        get() = supabaseUrl.isNotBlank() && supabaseAnonKey.isNotBlank()

    val isInnerTubeConfigured: Boolean
        get() = innerTubeApiKey.isNotBlank()
}

/**
 * Display version shown in the About section. Keep in sync with the canonical
 * `version.txt` at the repository root (currently 0.0.1-alpha).
 */
const val APP_VERSION = "0.0.1-alpha"
