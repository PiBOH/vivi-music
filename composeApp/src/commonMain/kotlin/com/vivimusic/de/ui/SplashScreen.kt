package com.vivimusic.de.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vivimusic.de.resources.*
import org.jetbrains.compose.resources.stringResource

/**
 * Startup screen shown briefly while the app initializes. It presents the
 * Axolotl mascot and the app name.
 */
@Composable
fun SplashScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AxolotlMascot(modifier = Modifier.size(160.dp))
        Text(
            text = stringResource(Res.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.size(20.dp))
        CircularProgressIndicator()
        Spacer(modifier = Modifier.size(12.dp))
        Text(
            text = stringResource(Res.string.startup_loading),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
