package com.vivimusic.de.data

/**
 * Platform persistence for the Supabase auth session. The session is stored as
 * a JSON blob (serialized `UserSession`) so the user stays signed in across app
 * restarts. On the desktop JVM this is a `session.json` file next to the app
 * settings; other platforms can implement it differently.
 */
expect fun loadSessionJson(): String?

expect fun saveSessionJson(json: String)

expect fun clearSessionJson()
