package com.vivimusic.de.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vivimusic.de.data.AppContainer
import com.vivimusic.de.data.readSetting
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
                AppViewModel(container.repository, container.syncManager, container.scope, container.audioEngine)
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
 * Root scaffold, adapted for the desktop: a side navigation rail on the left
 * (Home / Search / Listen Together / Library), the active screen and the mini
 * player on the right, and Settings in the top bar action.
 */
@Composable
private fun AppRoot(viewModel: AppViewModel) {
    var screen by remember { mutableStateOf(Screen.Home) }
    var showFullPlayer by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            NavigationRail {
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
            }

            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.align(Alignment.CenterStart),
                    )
                    IconButton(
                        onClick = { screen = Screen.Settings },
                        modifier = Modifier.align(Alignment.CenterEnd),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(Res.string.nav_settings),
                        )
                    }
                }

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
}
