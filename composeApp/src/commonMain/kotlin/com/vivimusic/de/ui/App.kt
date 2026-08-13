package com.vivimusic.de.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vivimusic.de.data.AppContainer
import com.vivimusic.de.data.readSetting
import com.vivimusic.de.data.update.openUrl
import com.vivimusic.de.i18n.AppEnvironment
import com.vivimusic.de.i18n.customAppLocale
import com.vivimusic.de.resources.*
import com.vivimusic.de.ui.screens.HomeScreen
import com.vivimusic.de.ui.screens.LibraryScreen
import com.vivimusic.de.ui.screens.SearchScreen
import com.vivimusic.de.ui.screens.SettingsScreen
import com.vivimusic.de.ui.screens.TogetherScreen
import com.vivimusic.de.ui.theme.ViviTheme
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

private const val LANGUAGE_KEY = "app.language"

@Composable
fun App(container: AppContainer) {
    AppEnvironment {
        ViviTheme {
            val viewModel = remember {
                AppViewModel(container.repository, container.syncManager, container.scope, container.audioEngine, container.updateChecker)
            }
            var showSplash by remember { mutableStateOf(true) }
            LaunchedEffect(Unit) {
                val saved = readSetting(LANGUAGE_KEY)
                if (!saved.isNullOrBlank()) {
                    customAppLocale = saved
                }
                delay(1_200)
                showSplash = false
            }
            if (showSplash) {
                SplashScreen()
            } else {
                AppRoot(viewModel)
            }
        }
    }
}

private enum class Screen { Home, Search, Together, Library, Settings }

/**
 * Root scaffold, adapted for the desktop: a side navigation rail with the
 * Axolotl logo as header and the destinations Home / Search / Listen Together /
 * Library plus Settings (pinned to the bottom). The active screen and the mini
 * player live on the right.
 */
@Composable
private fun AppRoot(viewModel: AppViewModel) {
    var screen by remember { mutableStateOf(Screen.Home) }
    var showFullPlayer by remember { mutableStateOf(false) }
    val updateStatus by viewModel.updateStatus.collectAsState()
    var updateDismissed by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            NavigationRail(
                header = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                    ) {
                        AxolotlMascot(modifier = Modifier.size(44.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(Res.string.app_name),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                },
            ) {
                NavigationRailItem(
                    selected = screen == Screen.Home,
                    onClick = { screen = Screen.Home },
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    label = { Text(stringResource(Res.string.nav_home)) },
                )
                NavigationRailItem(
                    selected = screen == Screen.Search,
                    onClick = { screen = Screen.Search },
                    icon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    label = { Text(stringResource(Res.string.nav_search)) },
                )
                NavigationRailItem(
                    selected = screen == Screen.Together,
                    onClick = { screen = Screen.Together },
                    icon = { Icon(Icons.Filled.Group, contentDescription = null) },
                    label = { Text(stringResource(Res.string.nav_together)) },
                )
                NavigationRailItem(
                    selected = screen == Screen.Library,
                    onClick = { screen = Screen.Library },
                    icon = { Icon(Icons.Filled.LibraryMusic, contentDescription = null) },
                    label = { Text(stringResource(Res.string.nav_library)) },
                )
                Spacer(modifier = Modifier.weight(1f))
                NavigationRailItem(
                    selected = screen == Screen.Settings,
                    onClick = { screen = Screen.Settings },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text(stringResource(Res.string.nav_settings)) },
                )
            }

            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (screen) {
                        Screen.Home -> HomeScreen(viewModel)
                        Screen.Search -> SearchScreen(viewModel)
                        Screen.Together -> TogetherScreen()
                        Screen.Library -> LibraryScreen(viewModel)
                        Screen.Settings -> SettingsScreen(viewModel)
                    }
                }

                MiniPlayer(viewModel = viewModel, onExpand = { showFullPlayer = true })
            }
        }

        if (showFullPlayer) {
            FullPlayer(
                viewModel = viewModel,
                onCollapse = { showFullPlayer = false },
            )
        }
    }

    val latest = updateStatus?.latest
    if (updateStatus?.updateAvailable == true && !updateDismissed && latest != null) {
        AlertDialog(
            onDismissRequest = { updateDismissed = true },
            title = { Text(stringResource(Res.string.update_available)) },
            text = {
                Text("${stringResource(Res.string.update_available_message)} ${latest.tagName}")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        openUrl(latest.htmlUrl)
                        updateDismissed = true
                    },
                ) {
                    Text(stringResource(Res.string.update_download))
                }
            },
            dismissButton = {
                TextButton(onClick = { updateDismissed = true }) {
                    Text(stringResource(Res.string.update_dismiss))
                }
            },
        )
    }
}
