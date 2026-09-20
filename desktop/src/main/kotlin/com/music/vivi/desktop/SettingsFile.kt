package com.music.vivi.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

/**
 * The human-editable settings file, `~/.vivimusic/settings.json`.
 *
 * `device-sync.json` stays the app's own store (it also holds the queue, the
 * library, the account credentials, the pairing data and the histories), while
 * this file is a **mirror of the options only**: every value the user can
 * configure, with camelCase keys that match the app's own field names (the one
 * exception is `hide_custom_apk_download_button`, which has no UI switch).
 *
 * Both directions work:
 *
 *  - every change made in the app rewrites the file ([mirror], called by
 *    `DesktopSettings.save`),
 *  - editing the file by hand while the app is running applies the changed
 *    options immediately ([start] watches it) — no restart, and no need to touch
 *    `device-sync.json`.
 *
 * Keys that are not options (credentials, API keys, histories, queue, library,
 * playlists, pairing bookkeeping, window geometry, transient session data) are
 * never written here and are ignored if they appear, so a hand-edited file can
 * neither leak nor fake an account.
 */
object SettingsFile {

    /** Explanatory first line of the file (ignored when reading). */
    const val COMMENT_KEY = "_comment"

    private const val COMMENT =
        "VIVI Music DE - options only. Edit and save while the app is running: " +
            "every value here is applied immediately. Playlists, queue, history, " +
            "account/credentials and pairing data live in device-sync.json."

    /** How often the file is checked for edits made outside the app. */
    private const val POLL_MS = 1_500L

    private val json = sharedJsonPretty

    /**
     * Everything that is NOT an option and therefore never appears in
     * settings.json: credentials, API keys, histories, queue/library/playlists,
     * pairing + sync bookkeeping, window/widget geometry, transient session
     * state and the values backed up around Data saver.
     */
    private val excluded = setOf(
        // Pairing / sync bookkeeping.
        "deviceId", "deviceName", "pairId", "serverUrl", "settings", "language",
        "languageSeq", "languagePeerId", "languagePeerSeq",
        // Account and credentials.
        "cookie", "dataSyncId", "visitorData", "accountName", "accountEmail",
        "accountChannelHandle", "aiApiKey", "deeplApiKey", "lastfmSession",
        // Histories and collections (data, not options).
        "searchHistory", "notificationHistory", "recognitionHistory",
        "library", "queueJson", "queueIndex",
        // Live Listen Together session state (the options themselves stay).
        "listenTogetherSessionToken", "listenTogetherRoomCode", "listenTogetherUserId",
        "listenTogetherIsHost", "listenTogetherSessionTimestamp", "listenTogetherBlockedUsers",
        // Window / widget geometry and one-shot bookkeeping.
        "windowMaximized", "windowX", "windowY", "windowWidth", "windowHeight",
        "widgetX", "widgetY", "firstLaunchDate",
        // Values parked while Data saver is on.
        "dataSaverBackupCanvas", "dataSaverBackupRotating",
    )

    /** Every field of [DesktopSyncState], read once from an empty instance. */
    private val allKeys: Set<String> by lazy {
        json.encodeToJsonElement(DesktopSyncState.serializer(), DesktopSyncState()).jsonObject.keys
    }

    /** The option keys this file mirrors (camelCase field names). */
    val allowed: Set<String> get() = allKeys - excluded

    private val file: File by lazy {
        File(System.getProperty("user.home"), ".vivimusic/settings.json").apply { parentFile?.mkdirs() }
    }

    private val _revision = MutableStateFlow(0L)

    /** Bumped every time the file was edited from outside the app. */
    val revision: StateFlow<Long> = _revision.asStateFlow()

    private val lock = Any()
    private var started = false
    private var lastSeenHash = ""
    private var lastWrittenHash = ""

    // ------------------------------------------------------------------
    // Reading / writing
    // ------------------------------------------------------------------

    /** True when the file holds an option (so a stray key cannot inject one). */
    private fun isAllowed(key: String): Boolean = key in allowed

    /** Renders [state] as the contents of settings.json (options only). */
    fun encode(state: DesktopSyncState): String {
        val all = json.encodeToJsonElement(DesktopSyncState.serializer(), state).jsonObject
        val out = buildJsonObject {
            put(COMMENT_KEY, COMMENT)
            for ((key, value) in all) if (isAllowed(key)) put(key, value)
        }
        return json.encodeToString(JsonObject.serializer(), out)
    }

    /** Writes the mirror when it does not already hold exactly [state]. */
    fun mirror(state: DesktopSyncState) {
        val text = encode(state)
        val hash = sha1(text)
        synchronized(lock) {
            if (hash == lastWrittenHash) return
            // The file may already be what we are about to write (the user just
            // edited it and we applied it): leave their formatting alone.
            if (normalizedFileText() == text) {
                lastWrittenHash = hash
                lastSeenHash = hash
                return
            }
            writeAtomically(text)
            lastWrittenHash = hash
            lastSeenHash = hash
        }
    }

    /** The file parsed and re-encoded (options only), or null when unusable. */
    private fun normalizedFileText(): String? = runCatching {
        val patch = json.parseToJsonElement(file.readText()).jsonObject
        val out = buildJsonObject {
            put(COMMENT_KEY, COMMENT)
            for ((key, value) in patch) if (isAllowed(key)) put(key, value)
        }
        json.encodeToString(JsonObject.serializer(), out)
    }.getOrNull()

    private fun writeAtomically(text: String) {
        runCatching {
            val tmp = File(file.parentFile, "settings.json.tmp")
            tmp.writeText(text)
            Files.move(
                tmp.toPath(),
                file.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
            )
        }.onFailure {
            // Fall back to a plain write (a reader may catch a partial file, in
            // which case it is skipped as invalid until the next save).
            runCatching { file.writeText(text) }
        }
    }

    // ------------------------------------------------------------------
    // External edits (settings.json edited by hand)
    // ------------------------------------------------------------------

    /**
     * Applies the options in the file over [state]. Only known option keys are
     * honoured; a value of the wrong type makes the whole patch be ignored
     * (rather than half-applied) and logs why.
     */
    fun merge(patch: JsonObject, state: DesktopSyncState): DesktopSyncState {
        val clean = JsonObject(patch.filterKeys { isAllowed(it) })
        if (clean.isEmpty()) return state
        return runCatching {
            val base = json.encodeToJsonElement(DesktopSyncState.serializer(), state).jsonObject
            val merged = JsonObject(HashMap(base).apply { putAll(clean) })
            json.decodeFromString(DesktopSyncState.serializer(), merged.toString())
        }.getOrElse { error ->
            AppLog.log("settings", "settings.json: ignoring the file - ${error.message ?: error::class.simpleName}")
            state
        }
    }

    /**
     * Reads the file and, when it changed since the last look, applies its
     * options. Returns true when something was actually applied.
     */
    fun applyExternal(): Boolean {
        val bytes = runCatching { file.readBytes() }.getOrNull() ?: return false
        val hash = sha1(bytes)
        val clean: JsonObject? = synchronized(lock) {
            if (hash == lastSeenHash || hash == lastWrittenHash) return false
            lastSeenHash = hash
            runCatching {
                json.parseToJsonElement(String(bytes, Charsets.UTF_8)).jsonObject
            }.getOrNull()
        }
        val patch = clean ?: run {
            AppLog.log("settings", "settings.json is not valid JSON - ignoring it until it is fixed")
            return false
        }
        var changed = false
        DesktopSettings.update { current ->
            val merged = merge(patch, current)
            changed = merged != current
            merged
        }
        if (changed) {
            AppLog.log("settings", "settings.json edited on disk: options applied")
            _revision.value = _revision.value + 1
        }
        return changed
    }

    // ------------------------------------------------------------------
    // Startup
    // ------------------------------------------------------------------

    /**
     * Adopts a hand-edited file, creates it when missing and starts watching for
     * edits. Safe to call more than once; must run before the first
     * `DesktopSettings.load()` of the session so a file edited while the app was
     * closed is honoured at startup.
     */
    fun start() {
        synchronized(lock) {
            if (started) return
            started = true
        }
        applyExternal()
        mirror(DesktopSettings.load())
        Thread(
            {
                while (!Thread.currentThread().isInterrupted) {
                    try {
                        Thread.sleep(POLL_MS)
                        applyExternal()
                    } catch (_: InterruptedException) {
                        return@Thread
                    } catch (_: Exception) {
                        // Never let the watcher die: keep polling.
                    }
                }
            },
            "vivi-settings-json",
        ).apply { isDaemon = true }.start()
    }

    private fun sha1(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-1").digest(bytes).joinToString("") { "%02x".format(it) }

    private fun sha1(text: String): String = sha1(text.toByteArray(Charsets.UTF_8))
}

/**
 * [SettingsFile.revision] as Compose state.
 *
 * Used as a `remember` key on every option read from the settings file: when the
 * file is edited from outside the app, those reads are re-initialised, so the
 * change is visible immediately instead of at the next restart.
 */
@Composable
fun settingsFileRevision(): Long = SettingsFile.revision.collectAsState().value
