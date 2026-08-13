package com.vivimusic.de.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vivimusic.de.data.AppContainer
import com.vivimusic.de.data.readSetting
import com.vivimusic.de.i18n.AppEnvironment
import com.vivimusic.de.i18n.customAppLocale
import com.vivimusic.de.resources.*
import com.vivimusic.de.ui.screens.HomeScreen
import com.vivimusic.de.ui.screens.LibraryScreen
import com.vivimusic.de.ui.screens.SettingsScreen
import com.vivimusic.de.ui.theme.ViviTheme
import org.jetbrains.compose.resources.stringResource

private const val LANGUAGE_KEY = "app.language"

@Composable
fun App(container: AppContainer) {
    AppEnvironment {
        ViviTheme {
            val viewModel = remember {
                AppViewModel(container.repository, container.syncManager, container.scope)
            }
            LaunchedEffect(Unit) {
                val saved = readSetting(LANGUAGE_KEY)
                if (!saved.isNullOrBlank()) {
                    customAppLocale = saved
                }
            }
            AppRoot(viewModel)
        }
    }
}

private enum class Screen { Home, Library, Settings }

@Composable
private fun AppRoot(viewModel: AppViewModel) {
    var screen by remember { mutableStateOf(Screen.Home) }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = stringResource(Res.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp),
        )
        TabRow(selectedTabIndex = screen.ordinal) {
            Tab(
                selected = screen == Screen.Home,
                onClick = { screen = Screen.Home },
                text = { Text(stringResource(Res.string.nav_home)) },
            )
            Tab(
                selected = screen == Screen.Library,
                onClick = { screen = Screen.Library },
                text = { Text(stringResource(Res.string.nav_library)) },
            )
            Tab(
                selected = screen == Screen.Settings,
                onClick = { screen = Screen.Settings },
                text = { Text(stringResource(Res.string.nav_settings)) },
            )
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (screen) {
                Screen.Home -> HomeScreen(viewModel)
                Screen.Library -> LibraryScreen(viewModel)
                Screen.Settings -> SettingsScreen(viewModel)
            }
        }

        PlayerBar(viewModel)
    }
}
