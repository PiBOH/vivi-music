package com.vivimusic.de.ui

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.vivimusic.de.resources.*
import org.jetbrains.compose.resources.painterResource

/**
 * Renders the Axolotl mascot from the bundled `logo.png` resource.
 *
 * This is the single in-app source for the mascot (splash screen and About
 * section). The same image is the source for the desktop app icons, produced by
 * `tools/generate_icons.py`.
 */
@Composable
fun AxolotlMascot(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(Res.drawable.logo),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier,
    )
}
