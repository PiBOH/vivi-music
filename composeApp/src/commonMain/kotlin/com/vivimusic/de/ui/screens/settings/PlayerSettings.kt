package com.vivimusic.de.ui.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.vivimusic.de.resources.*
import org.jetbrains.compose.resources.stringResource

/** Player and audio settings: audio quality and playback toggles. */
@Composable
fun PlayerSettings(onBack: () -> Unit) {
    var showQualityDialog by remember { mutableStateOf(false) }
    val (audioQuality, setAudioQuality) = rememberStringSetting("audio.quality", "auto")
    val (normalization, setNormalization) = rememberBoolSetting("audio.normalization", true)
    val (skipSilence, setSkipSilence) = rememberBoolSetting("audio.skip_silence", false)
    val (crossfade, setCrossfade) = rememberBoolSetting("audio.crossfade", false)

    val qualityLabel = when (audioQuality) {
        "high" -> stringResource(Res.string.audio_quality_high)
        "low" -> stringResource(Res.string.audio_quality_low)
        else -> stringResource(Res.string.audio_quality_auto)
    }

    SettingsPage(
        title = stringResource(Res.string.player_and_audio),
        onBack = onBack,
    ) {
        SettingsGroup(title = stringResource(Res.string.player_and_audio)) {
            SettingsItem(
                title = stringResource(Res.string.audio_quality),
                trailingText = qualityLabel,
                onClick = { showQualityDialog = true },
            )
            SettingsDivider()
            SettingsItem(
                title = stringResource(Res.string.audio_normalization),
                checked = normalization,
                onCheckedChange = setNormalization,
            )
            SettingsDivider()
            SettingsItem(
                title = stringResource(Res.string.skip_silence),
                checked = skipSilence,
                onCheckedChange = setSkipSilence,
            )
            SettingsDivider()
            SettingsItem(
                title = stringResource(Res.string.crossfade),
                checked = crossfade,
                onCheckedChange = setCrossfade,
            )
        }
    }

    if (showQualityDialog) {
        ChoiceDialog(
            title = stringResource(Res.string.audio_quality),
            options = listOf(
                "auto" to stringResource(Res.string.audio_quality_auto),
                "high" to stringResource(Res.string.audio_quality_high),
                "low" to stringResource(Res.string.audio_quality_low),
            ),
            selectedValue = audioQuality,
            onSelect = setAudioQuality,
            onDismiss = { showQualityDialog = false },
        )
    }
}
