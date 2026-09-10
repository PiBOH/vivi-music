package com.music.vivi.desktop

import kotlinx.serialization.json.Json

/**
 * Shared JSON codecs. The plain `ignoreUnknownKeys` config used to be
 * copy-pasted across several persistence/network files; the variants below
 * differ only in the extra flags each consumer needs.
 */
val sharedJson = Json { ignoreUnknownKeys = true }
val sharedJsonEncodeDefaults = Json { ignoreUnknownKeys = true; encodeDefaults = true }
val sharedJsonLenient = Json { ignoreUnknownKeys = true; encodeDefaults = true; isLenient = true }
val sharedJsonPretty = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = true }

/** Human-readable byte size; negative = unknown → "—". */
internal fun formatBytes(bytes: Long): String {
    if (bytes < 0) return "—"
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    if (kb / 1024.0 < 1024) return "%.1f MB".format(kb / 1024.0)
    return "%.2f GB".format(kb / 1024.0 / 1024.0)
}

/** Human-readable bytes-per-second; negative = unknown → "—". */
internal fun formatSpeed(bps: Long): String = if (bps < 0) "—" else "${formatBytes(bps)}/s"