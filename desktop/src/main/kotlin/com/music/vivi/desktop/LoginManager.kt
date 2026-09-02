package com.music.vivi.desktop

import com.music.innertube.YouTube
import com.music.innertube.models.AccountInfo
import com.music.innertube.models.YouTubeClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Desktop YouTube authentication. The desktop has no WebView, so the user logs
 * into music.youtube.com in their browser and pastes the `Cookie` header here.
 * We then extract the account's `DATASYNC_ID` and `VISITOR_DATA` from the
 * music.youtube.com shell and validate the session via `YouTube.accountInfo()`.
 *
 * The cookie is stored locally in `~/.vivimusic/device-sync.json` (same as the
 * Android app, which keeps it in SharedPreferences).
 */
object LoginManager {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val authCookieNames = listOf("SAPISID", "__Secure-3PAPISID", "__Secure-1PAPISID")

    fun isLoggedIn(): Boolean {
        val cookie = YouTube.cookie.orEmpty()
        return cookie.isNotBlank() && authCookieNames.any { it in cookie }
    }

    /** Restores persisted credentials into the [YouTube] singleton at startup. */
    fun restore() {
        val s = DesktopSettings.load()
        if (s.cookie.isBlank()) return
        YouTube.cookie = s.cookie
        YouTube.dataSyncId = s.dataSyncId.ifBlank { null }
        YouTube.visitorData = s.visitorData.ifBlank { null }
        YouTube.useLoginForBrowse = true
    }

    /**
     * Logs in with a pasted `Cookie` header. [dataSyncIdOverride] and
     * [visitorDataOverride] are optional manual fallbacks used when the
     * automatic extraction from the music.youtube.com shell fails. Throws with
     * a readable message on failure. Returns the validated account info.
     */
    suspend fun login(
        cookie: String,
        dataSyncIdOverride: String? = null,
        visitorDataOverride: String? = null,
    ): AccountInfo = withContext(Dispatchers.IO) {
        val trimmed = cookie.trim()
        if (trimmed.isBlank()) throw IllegalArgumentException("Cookie is empty")
        if (authCookieNames.none { it in trimmed }) {
            throw IllegalArgumentException("Cookie is missing SAPISID — paste the full Cookie header")
        }

        // Right after the embedded sign-in the session can still be settling
        // (the WebView now waits, but a retry costs little): try the whole
        // validation twice, refreshing the parsed ids on the retry.
        //
        // DATASYNC_ID and VISITOR_DATA are MANDATORY, not optional: innertube
        // puts them in the API context (visitorData + onBehalfOfUser) and the
        // account validation answers as guest (cryptic NPE or 5xx) without
        // them. The ids come from the sign-in page itself (the WebView reads
        // them from ytcfg) or from the music.youtube.com shell below; if both
        // sources miss them the login fails fast with a readable E1030 instead
        // of a confusing backend error.
        var lastError: Throwable? = null
        repeat(2) { attempt ->
            try {
                YouTube.cookie = trimmed
                val (extractedDataSyncId, extractedVisitorData) = extractAccountIds(trimmed)
                val dataSyncId = dataSyncIdOverride?.trim()?.takeIf { it.isNotBlank() } ?: extractedDataSyncId
                val visitorData = visitorDataOverride?.trim()?.takeIf { it.isNotBlank() } ?: extractedVisitorData
                val missingIds = listOfNotNull(
                    if (dataSyncId.isNullOrBlank()) "DATASYNC_ID" else null,
                    if (visitorData.isNullOrBlank()) "VISITOR_DATA" else null,
                )
                if (missingIds.isNotEmpty()) {
                    throw IllegalStateException(
                        "E1030 ${missingIds.joinToString(" and ")} could not be extracted and is " +
                            "required for a valid session. Retry the sign-in, or paste the values " +
                            "manually in the manual cookie section."
                    )
                }
                YouTube.dataSyncId = dataSyncId
                YouTube.visitorData = visitorData
                YouTube.useLoginForBrowse = true

                val account = YouTube.accountInfo().getOrThrow()
                DesktopSettings.update {
                    it.copy(
                        cookie = trimmed,
                        dataSyncId = dataSyncId.orEmpty(),
                        visitorData = visitorData.orEmpty(),
                        accountName = account.name,
                        accountEmail = account.email.orEmpty(),
                        accountChannelHandle = account.channelHandle.orEmpty(),
                    )
                }
                return@withContext account
            } catch (e: Exception) {
                lastError = e
                if (attempt == 0) {
                    try {
                        Thread.sleep(2000)
                    } catch (_: InterruptedException) {
                    }
                }
            }
        }

        YouTube.cookie = null
        val rawDetail = lastError?.message?.takeIf { it.isNotBlank() }
            ?: lastError?.javaClass?.simpleName
            ?: "unknown error"
        // A Ktor server exception surfaces as "Server error(POST <url>: 5xx. Text: ...)".
        // That is a Google backend error, not a credential problem — tag it with
        // its own code (E1029, see ERRORS.md) so the user can look it up instead
        // of assuming their cookie/session is broken.
        val detail = if (rawDetail.startsWith("Server error") && Regex("\\b5\\d\\d\\b").containsMatchIn(rawDetail)) {
            "E1029 $rawDetail"
        } else {
            rawDetail
        }
        // Append the failure to the same debug file used by the WebView capture,
        // so the next user report tells us exactly what went wrong.
        runCatching {
            val f = File(System.getProperty("user.home"), ".vivimusic/login-debug.log")
            f.parentFile?.mkdirs()
            f.appendText(
                "[${java.time.LocalDateTime.now()}] login validation failed: $detail " +
                    "(${lastError?.javaClass?.name})\n"
            )
        }
        throw IllegalStateException("Login validation failed: $detail")
    }

    fun logout() {
        YouTube.cookie = null
        YouTube.dataSyncId = null
        YouTube.visitorData = null
        YouTube.useLoginForBrowse = false
        DesktopSettings.update {
            it.copy(
                cookie = "",
                dataSyncId = "",
                visitorData = "",
                accountName = "",
                accountEmail = "",
                accountChannelHandle = "",
            )
        }
    }

    /**
     * Fetches the YouTube Music shell with the session cookie and extracts
     * `DATASYNC_ID` (delegated account id) and `VISITOR_DATA` from the page.
     * Tries the music shell first, then the plain www shell — both embed the
     * same ytcfg block when a session cookie is sent.
     */
    private suspend fun extractAccountIds(cookie: String): Pair<String?, String?> =
        withContext(Dispatchers.IO) {
            for (url in listOf("https://music.youtube.com/", "https://www.youtube.com/")) {
                val ids = fetchShellIds(url, cookie)
                if (ids.first != null || ids.second != null) return@withContext ids
            }
            null to null
        }

    private fun fetchShellIds(url: String, cookie: String): Pair<String?, String?> =
        runCatching {
            val request = Request.Builder()
                .url(url)
                .header("Cookie", cookie)
                .header("User-Agent", YouTubeClient.USER_AGENT_WEB)
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@runCatching null to null
                val body = response.body.string()
                val dataSyncId = Regex("\"DATASYNC_ID\"\\s*:\\s*\"([^\"]+)\"")
                    .find(body)?.groupValues?.get(1)
                val visitorData = Regex("\"VISITOR_DATA\"\\s*:\\s*\"([^\"]+)\"")
                    .find(body)?.groupValues?.get(1)
                dataSyncId to visitorData
            }
        }.getOrElse { null to null }
}
