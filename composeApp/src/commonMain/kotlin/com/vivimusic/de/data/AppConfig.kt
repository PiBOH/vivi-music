package com.vivimusic.de.data

/**
 * Runtime configuration, populated by the desktop entry point (`main.kt`).
 *
 * Values come from the `SUPABASE_URL` and `SUPABASE_ANON_KEY` environment
 * variables or from a `supabase.env` file next to the executable.
 *
 * When the values are empty, remote sync is disabled and the app runs in a
 * local-only mode.
 */
object AppConfig {
    var supabaseUrl: String = ""
    var supabaseAnonKey: String = ""

    val isSyncConfigured: Boolean
        get() = supabaseUrl.isNotBlank() && supabaseAnonKey.isNotBlank()
}

/**
 * Display version shown in the About section. Keep in sync with the canonical
 * `version.txt` at the repository root (currently 0.0.1-alpha).
 */
const val APP_VERSION = "0.0.1-alpha"
