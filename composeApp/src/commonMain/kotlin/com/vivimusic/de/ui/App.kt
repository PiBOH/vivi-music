package com.vivimusic.de.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
                AppViewModel(container.repository, container.syncManager, container.scope)
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
 * Root scaffold, mirroring the mobile layout: a top bar, the active screen,
 * the mini player and the bottom navigation bar (Home / Search / Listen
 * Together / Library). Settings is reached from the top bar action.
 */
@Composable
private fun AppRoot(viewModel: AppViewModel) {
    var screen by remember { mutableStateOf(Screen.Home) }

    Column(modifier = Modifier.fillMaxSize()) {
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

        PlayerBar(viewModel)

        NavigationBar {
            NavigationBarItem(
                selected = screen == Screen.Home,
                onClick = { screen = Screen.Home },
                icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                label = { Text(stringResource(Res.string.nav_home)) },
            )
            NavigationBarItem(
                selected = screen == Screen.Search,
                onClick = { screen = Screen.Search },
                icon = { Icon(Icons.Filled.Search, contentDescription = null) },
                label = { Text(stringResource(Res.string.nav_search)) },
            )
            NavigationBarItem(
                selected = screen == Screen.Together,
                onClick = { screen = Screen.Together },
                icon = { Icon(Icons.Filled.Group, contentDescription = null) },
                label = { Text(stringResource(Res.string.nav_together)) },
            )
            NavigationBarItem(
                selected = screen == Screen.Library,
                onClick = { screen = Screen.Library },
                icon = { Icon(Icons.Filled.LibraryMusic, contentDescription = null) },
                label = { Text(stringResource(Res.string.nav_library)) },
            )
        }
    }
}
