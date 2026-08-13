package com.vivimusic.de.data.update

import com.vivimusic.de.data.AppConfig
import com.vivimusic.de.data.network.sharedJson
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * A single GitHub release, with only the fields the update checker needs.
 */
@Serializable
data class AppAsset(
    val name: String,
    @SerialName("browser_download_url") val downloadUrl: String,
)

@Serializable
data class AppRelease(
    @SerialName("tag_name") val tagName: String,
    val name: String? = null,
    val prerelease: Boolean = false,
    @SerialName("html_url") val htmlUrl: String = "",
    @SerialName("published_at") val publishedAt: String? = null,
    val assets: List<AppAsset> = emptyList(),
)

/**
 * Result of an update check: the selected latest release and whether it is
 * newer than the running app version.
 */
data class UpdateStatus(
    val latest: AppRelease? = null,
    val updateAvailable: Boolean = false,
)

sealed interface UpdateDownloadState {
    data object Idle : UpdateDownloadState
    data object Downloading : UpdateDownloadState
    data class Launched(val path: String) : UpdateDownloadState
    data class Error(val message: String) : UpdateDownloadState
}

/**
 * Checks the GitHub Releases API for the latest release and compares it against
 * [AppConfig.appVersion] using SemVer.
 */
class UpdateChecker(
    private val httpClient: HttpClient,
    private val json: Json = sharedJson,
) {
    /**
     * Returns the latest release that satisfies the filter: stable releases by
     * default, or any release (including pre-releases) when
     * [includePrereleases] is true.
     */
    suspend fun check(includePrereleases: Boolean): UpdateStatus {
        val releases = fetchReleases()
        val candidate = releases.firstOrNull { includePrereleases || !it.prerelease }
            ?: return UpdateStatus()
        val updateAvailable = isNewer(candidate.tagName, AppConfig.appVersion)
        return UpdateStatus(latest = candidate, updateAvailable = updateAvailable)
    }

    private suspend fun fetchReleases(): List<AppRelease> = try {
        val body = httpClient.get("$GITHUB_API/repos/$REPOSITORY/releases") {
            header("User-Agent", "vivi-music-de-updater")
            header("Accept", "application/vnd.github+json")
        }.bodyAsText()
        json.decodeFromString<List<AppRelease>>(body)
    } catch (e: Exception) {
        emptyList()
    }

    /**
     * Downloads the release asset matching this operating system and starts it
     * with the native installer/launcher. The browser is never opened.
     */
    suspend fun downloadAndLaunch(release: AppRelease): String {
        val asset = selectUpdateAsset(release.assets)
            ?: error("No update asset is available for this operating system")
        val response = httpClient.get(asset.downloadUrl) {
            header("User-Agent", "vivi-music-de-updater")
            header("Accept", "application/octet-stream")
        }
        if (response.status.value !in 200..299) {
            error("Update download failed: HTTP ${response.status.value}")
        }
        val bytes = response.body<ByteArray>()
        return saveAndLaunchUpdate(asset.name, bytes)
    }

    private fun selectUpdateAsset(assets: List<AppAsset>): AppAsset? {
        val priorities = updateAssetSuffixes()
        return priorities.asSequence()
            .mapNotNull { suffix ->
                assets.firstOrNull { asset -> asset.name.endsWith(suffix, ignoreCase = true) }
            }
            .firstOrNull()
    }

    /** Fetches the raw `CHANGELOG.md` from the repository (Keep a Changelog). */
    suspend fun fetchChangelogMarkdown(): String = try {
        httpClient.get("$RAW_BASE/$REPOSITORY/main/CHANGELOG.md") {
            header("User-Agent", "vivi-music-de-updater")
        }.bodyAsText()
    } catch (e: Exception) {
        ""
    }

    companion object {
        private const val GITHUB_API = "https://api.github.com"
        private const val RAW_BASE = "https://raw.githubusercontent.com"
        private const val REPOSITORY = "PiBOH/vivi-music-de"
    }
}

/** Opens a URL in the system default browser for source links. */
expect fun openUrl(url: String)

/** Returns update asset suffixes in preference order for the current OS. */
expect fun updateAssetSuffixes(): List<String>

/** Saves the downloaded asset and starts the native installer/launcher. */
expect fun saveAndLaunchUpdate(fileName: String, bytes: ByteArray): String

// ----- SemVer comparison -----

private data class ParsedVersion(val core: List<Int>, val pre: List<String>)

private fun isNewer(remote: String, current: String): Boolean {
    val r = parseVersion(remote) ?: return false
    val c = parseVersion(current) ?: return false
    val coreCompare = compareCore(r.core, c.core)
    if (coreCompare != 0) return coreCompare > 0
    return comparePreRelease(r.pre, c.pre) > 0
}

private fun parseVersion(version: String): ParsedVersion? {
    val trimmed = version.trim().removePrefix("v")
    val corePart = trimmed.substringBefore("-").substringBefore("+")
    val prePart = trimmed.substringAfter("-", "").substringBefore("+")
    val core = corePart.split(".").mapNotNull { it.toIntOrNull() }
    if (core.isEmpty()) return null
    val pre = if (prePart.isBlank()) emptyList() else prePart.split(".")
    return ParsedVersion(core, pre)
}

private fun compareCore(a: List<Int>, b: List<Int>): Int {
    val length = maxOf(a.size, b.size)
    for (i in 0 until length) {
        val av = a.getOrElse(i) { 0 }
        val bv = b.getOrElse(i) { 0 }
        if (av != bv) return av.compareTo(bv)
    }
    return 0
}

private fun comparePreRelease(a: List<String>, b: List<String>): Int {
    if (a.isEmpty() && b.isEmpty()) return 0
    // A version without a pre-release is newer than one with it.
    if (a.isEmpty()) return 1
    if (b.isEmpty()) return -1
    val length = minOf(a.size, b.size)
    for (i in 0 until length) {
        val aNum = a[i].toIntOrNull()
        val bNum = b[i].toIntOrNull()
        val cmp = when {
            aNum != null && bNum != null -> aNum.compareTo(bNum)
            aNum != null -> -1 // numeric identifiers have lower precedence
            bNum != null -> 1
            else -> a[i].compareTo(b[i])
        }
        if (cmp != 0) return cmp
    }
    return a.size.compareTo(b.size)
}
