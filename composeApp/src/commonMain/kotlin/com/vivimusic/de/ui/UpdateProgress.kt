package com.vivimusic.de.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vivimusic.de.data.update.UpdateDownloadProgress
import com.vivimusic.de.data.update.UpdateDownloadState
import com.vivimusic.de.resources.Res
import com.vivimusic.de.resources.update_download_percent
import com.vivimusic.de.resources.update_download_size
import com.vivimusic.de.resources.update_download_speed
import com.vivimusic.de.resources.update_download_unknown_size
import org.jetbrains.compose.resources.stringResource

/** Shows live percentage, transferred size and download speed for an update. */
@Composable
fun UpdateDownloadProgressPanel(
    state: UpdateDownloadState,
    modifier: Modifier = Modifier,
) {
    val downloading = state as? UpdateDownloadState.Downloading ?: return
    val progress = downloading.progress
    val percent = progress.percent?.let { "$it%" }
        ?: stringResource(Res.string.update_download_unknown_size)
    val total = progress.totalBytes?.let(::formatBytes)
        ?: stringResource(Res.string.update_download_unknown_size)

    Column(modifier = modifier.fillMaxWidth()) {
        progress.percent?.let { fraction ->
            LinearProgressIndicator(
                progress = { fraction / 100f },
                modifier = Modifier.fillMaxWidth(),
            )
        } ?: LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "${stringResource(Res.string.update_download_percent)}: $percent",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "${stringResource(Res.string.update_download_size)}: ${formatBytes(progress.downloadedBytes)} / $total",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = "${stringResource(Res.string.update_download_speed)}: ${formatBytes(progress.speedBytesPerSecond)}/s",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024L -> "$bytes B"
    bytes < 1024L * 1024L -> "${bytes / 1024L} KB"
    bytes < 1024L * 1024L * 1024L -> "${bytes / (1024L * 1024L)} MB"
    else -> "${bytes / (1024L * 1024L * 1024L)} GB"
}
