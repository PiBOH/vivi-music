package com.music.vivi.desktop

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/** Snapshot of an in-progress update download. */
data class DownloadProgress(
    val downloadedBytes: Long,
    val totalBytes: Long,
    val speedBytesPerSecond: Long,
) {
    val percent: Int
        get() = if (totalBytes <= 0) 0 else ((downloadedBytes * 100) / totalBytes).toInt()
}

/**
 * Downloads update installers into `~/.vivimusic/updates/` and reports progress
 * (percent + speed). Also exposes the list of downloaded installers and a way
 * to delete them.
 */
object UpdateDownloader {
    private const val INSTALLER_MAX_AGE_MS = 7L * 24L * 60L * 60L * 1000L

    /**
     * Suffix of a download that has not completed yet. The installer is written
     * to `<name>.part` and renamed only once every byte arrived, so a download
     * interrupted by leaving the screen — or by quitting the app — can never be
     * mistaken for a finished one. That was #82: leaving the Updates screen while
     * the download was running made it show "downloaded" on the way back, and
     * "open installer" then failed on a truncated file.
     */
    private const val PARTIAL_SUFFIX = ".part"

    /** A leftover `.part` older than this is from an interrupted run (a running
     *  download keeps refreshing its timestamp), so it can safely be removed. */
    private const val PARTIAL_MAX_AGE_MS = 60L * 60L * 1000L

    val updatesDir: File =
        File(System.getProperty("user.home"), ".vivimusic/updates").apply { mkdirs() }

    init {
        cleanupExpiredInstallers()
    }

    /**
     * Removes completed installer files older than seven days, plus the `.part`
     * files an interrupted download left behind.
     */
    fun cleanupExpiredInstallers(now: Long = System.currentTimeMillis()) {
        val cutoff = now - INSTALLER_MAX_AGE_MS
        val partialCutoff = now - PARTIAL_MAX_AGE_MS
        updatesDir.listFiles()
            ?.filter { it.isFile && it.lastModified() < cutoff }
            ?.forEach { it.delete() }
        updatesDir.listFiles()
            ?.filter {
                it.isFile && it.name.endsWith(PARTIAL_SUFFIX) && it.lastModified() < partialCutoff
            }
            ?.forEach { it.delete() }
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    /** Files previously downloaded by this updater (installers only). */
    fun downloadedInstallers(): List<File> = run {
        cleanupExpiredInstallers()
        updatesDir.listFiles()
            ?.filter { it.isFile && !it.name.endsWith(PARTIAL_SUFFIX) }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    /**
     * The already-downloaded installer for [fileName], if present — and, when
     * [expectedSizeBytes] is known, only if it is complete. A file whose size
     * does not match the release asset is a truncated download and must never
     * be offered as "open installer" (#82).
     */
    fun downloadedInstaller(fileName: String, expectedSizeBytes: Long = 0L): File? =
        File(updatesDir, fileName).takeIf {
            it.isFile && (expectedSizeBytes <= 0L || it.length() == expectedSizeBytes)
        }

    fun deleteAll() {
        updatesDir.listFiles()?.forEach { it.delete() }
    }

    fun delete(file: File) {
        file.delete()
    }

    /**
     * Downloads [url] to `updatesDir/[fileName]`, invoking [onProgress] as bytes
     * arrive. Returns the downloaded file. Throws on network errors, and also
     * when the transfer ends before [expectedSizeBytes] bytes arrived: the
     * partial file is deleted, never renamed (#82).
     */
    suspend fun download(
        url: String,
        fileName: String,
        expectedSizeBytes: Long = 0L,
        onProgress: (DownloadProgress) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        cleanupExpiredInstallers()
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "VIVIMusic-Desktop-Updater")
            .header("Accept", "application/octet-stream")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw java.io.IOException("HTTP ${response.code}")
            }
            val body = response.body
            val total = body.contentLength()
            val dest = File(updatesDir, fileName)
            dest.parentFile?.mkdirs()
            // Written under a `.part` name: the final name only ever exists for
            // a fully received installer, whoever looks at the directory.
            val partial = File(updatesDir, "$fileName$PARTIAL_SUFFIX")

            var downloaded = 0L
            var lastSampleAt = System.currentTimeMillis()
            var lastSampleBytes = 0L
            var speed = 0L

            partial.outputStream().use { out ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        out.write(buffer, 0, read)
                        downloaded += read

                        val now = System.currentTimeMillis()
                        val elapsed = now - lastSampleAt
                        if (elapsed >= 500) {
                            speed = ((downloaded - lastSampleBytes) * 1000L) / elapsed
                            lastSampleAt = now
                            lastSampleBytes = downloaded
                        }
                        onProgress(DownloadProgress(downloaded, total, speed))
                    }
                }
            }
            // A short read (connection dropped, app quitting) must not promote
            // the partial file: an installer that cannot start is worse than no
            // installer at all.
            val expected = if (expectedSizeBytes > 0L) expectedSizeBytes else total
            if (expected > 0L && downloaded != expected) {
                partial.delete()
                throw java.io.IOException("incomplete download: $downloaded of $expected bytes")
            }
            // The rename is the completion marker: `dest` appears only now.
            dest.delete()
            if (!partial.renameTo(dest)) {
                partial.copyTo(dest, overwrite = true)
                partial.delete()
            }
            dest
        }
    }
}
