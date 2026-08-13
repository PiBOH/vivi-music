package com.vivimusic.de.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import com.vivimusic.de.data.update.UpdateDownloadState
import com.vivimusic.de.i18n.AppEnvironment
import com.vivimusic.de.i18n.customAppLocale
import com.vivimusic.de.resources.*
import com.vivimusic.de.ui.screens.AccountScreen
import com.vivimusic.de.ui.screens.AlbumScreen
import com.vivimusic.de.ui.screens.ArtistScreen
import com.vivimusic.de.ui.screens.HistoryScreen
import com.vivimusic.de.ui.screens.HomeScreen
import com.vivimusic.de.ui.screens.LibraryScreen
import com.vivimusic.de.ui.screens.PlaylistScreen
import com.vivimusic.de.ui.screens.SearchScreen
import com.vivimusic.de.ui.screens.SettingsScreen
import com.vivimusic.de.ui.screens.TogetherScreen
import com.vivimusic.de.ui.theme.ViviTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource

private const val LANGUAGE_KEY = "app.language"

/**
 * Starts the visual shell first, then constructs the database, HTTP client and
 * sync services off the UI thread. This keeps the native window responsive on
 * older CPUs while the heavier application services initialize.
 */
@Composable
fun AppStartup(
    scope: CoroutineScope,
    createContainer: () -> AppContainer,
) {
    AppEnvironment {
        ViviTheme {
            var container by remember { mutableStateOf<AppContainer?>(null) }
            var startupError by remember { mutableStateOf<Throwable?>(null) }

            LaunchedEffect(Unit) {
                try {
                    container = withContext(Dispatchers.Default) {
                        createContainer().also { it.start() }
                    }
                } catch (throwable: Throwable) {
                    startupError = throwable
                }
            }

            when {
                container != null -> AppContent(container!!)
                startupError != null -> StartupError(startupError!!)
                else -> SplashScreen()
            }
        }
    }
}

@Composable
private fun StartupError(throwable: Throwable) {
    val details = remember(throwable) {
        buildString {
            append(throwable::class.simpleName ?: "Throwable")
            append(": ")
            append(throwable.message ?: "No error message")
            appendLine()
            appendLine()
            append(throwable.stackTraceToString())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AxolotlMascot(modifier = Modifier.size(128.dp))
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = stringResource(Res.string.startup_error),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.error,
        )
        TextButton(onClick = { copyToClipboard(details) }) {
            Text(stringResource(Res.string.copy_error))
        }
        SelectionContainer {
            Text(
                text = details,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        TextButton(onClick = { copyToClipboard(details) }) {
            Text(stringResource(Res.string.copy_error))
        }
    }
}

@Composable
fun App(container: AppContainer) {
    AppEnvironment {
        ViviTheme {
            AppContent(container)
        }
    }
}

@Composable
private fun AppContent(container: AppContainer) {
    val viewModel = remember {
        AppViewModel(container.repository, container.syncManager, container.scope, container.audioEngine, container.updateChecker)
    }
    // Restore the saved language, then show the app immediately. There is no
    // artificial splash delay: the window opens as fast as the initial
    // composition allows.
    LaunchedEffect(Unit) {
        val saved = readSetting(LANGUAGE_KEY)
        if (!saved.isNullOrBlank()) {
            customAppLocale = saved
        }
        // Defer the initial network work (home feed, update check, auth
        // restore) until after the first frame is composed.
        viewModel.loadInitialData()
    }
    AppRoot(viewModel)
}

private enum class Screen { Home, Search, Together, Library, History, Account, Settings }

/** A detail destination shown in place of the main content area. */
private sealed interface Detail {
    data class AlbumDetail(val browseId: String?) : Detail
    data class ArtistDetail(val browseId: String?) : Detail
    data class PlaylistDetail(val playlistId: String) : Detail
}

/**
 * Root scaffold, adapted for the desktop: a side navigation rail with the
 * Axolotl logo as header and the destinations Home / Search / Listen Together /
 * Library plus Settings (pinned to the bottom). The active screen and the mini
 * player live on the right.
 */
@Composable
private fun AppRoot(viewModel: AppViewModel) {
    var screen by remember { mutableStateOf(Screen.Home) }
    var detail by remember { mutableStateOf<Detail?>(null) }
    var showFullPlayer by remember { mutableStateOf(false) }
    val updateStatus by viewModel.updateStatus.collectAsState()
    val updateDownloadState by viewModel.updateDownloadState.collectAsState()
    var updateDismissed by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(modifier = Modifier.fillMaxSize()) {
            NavigationRail(
                // Material3 1.4 sizes the rail from its content, so a
                // fillMaxWidth header would stretch it across the window.
                // Pin the rail to the standard 80dp collapsed width.
                modifier = Modifier.width(80.dp),
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
                    selected = screen == Screen.Home && detail == null,
                    onClick = { screen = Screen.Home; detail = null },
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    label = { Text(stringResource(Res.string.nav_home)) },
                )
                NavigationRailItem(
                    selected = screen == Screen.Search && detail == null,
                    onClick = { screen = Screen.Search; detail = null },
                    icon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    label = { Text(stringResource(Res.string.nav_search)) },
                )
                NavigationRailItem(
                    selected = screen == Screen.Together && detail == null,
                    onClick = { screen = Screen.Together; detail = null },
                    icon = { Icon(Icons.Filled.Group, contentDescription = null) },
                    label = { Text(stringResource(Res.string.nav_together)) },
                )
                NavigationRailItem(
                    selected = screen == Screen.Library && detail == null,
                    onClick = { screen = Screen.Library; detail = null },
                    icon = { Icon(Icons.Filled.LibraryMusic, contentDescription = null) },
                    label = { Text(stringResource(Res.string.nav_library)) },
                )
                NavigationRailItem(
                    selected = screen == Screen.History,
                    onClick = { screen = Screen.History; detail = null },
                    icon = { Icon(Icons.Filled.History, contentDescription = null) },
                    label = { Text(stringResource(Res.string.nav_history)) },
                )
                NavigationRailItem(
                    selected = screen == Screen.Account && detail == null,
                    onClick = { screen = Screen.Account; detail = null },
                    icon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    label = { Text(stringResource(Res.string.account)) },
                )
                Spacer(modifier = Modifier.weight(1f))
                NavigationRailItem(
                    selected = screen == Screen.Settings,
                    onClick = { screen = Screen.Settings; detail = null },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text(stringResource(Res.string.nav_settings)) },
                )
            }

            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (val current = detail) {
                        is Detail.AlbumDetail -> AlbumScreen(
                            viewModel = viewModel,
                            onBack = { detail = null },
                        )
                        is Detail.ArtistDetail -> ArtistScreen(
                            viewModel = viewModel,
                            onBack = { detail = null },
                        )
                        is Detail.PlaylistDetail -> PlaylistScreen(
                            viewModel = viewModel,
                            onBack = { detail = null },
                        )
                        null -> when (screen) {
                            Screen.Home -> HomeScreen(viewModel)
                            Screen.Search -> SearchScreen(viewModel)
                            Screen.Together -> TogetherScreen()
                            Screen.Library -> LibraryScreen(
                                viewModel = viewModel,
                                onOpenAlbum = { album ->
                                    viewModel.showLocalAlbum(album)
                                    detail = Detail.AlbumDetail(browseId = null)
                                },
                                onOpenArtist = { artist ->
                                    viewModel.showLocalArtist(artist)
                                    detail = Detail.ArtistDetail(browseId = null)
                                },
                                onOpenPlaylist = { playlist ->
                                    viewModel.openPlaylist(playlist)
                                    detail = Detail.PlaylistDetail(playlist.id)
                                },
                            )
                            Screen.History -> HistoryScreen(viewModel)
                            Screen.Account -> AccountScreen(
                                viewModel = viewModel,
                                onOpenAlbum = { browseId ->
                                    viewModel.openAlbum(browseId)
                                    detail = Detail.AlbumDetail(browseId)
                                },
                                onOpenArtist = { browseId ->
                                    viewModel.openArtist(browseId)
                                    detail = Detail.ArtistDetail(browseId)
                                },
                            )
                            Screen.Settings -> SettingsScreen(viewModel)
                        }
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
        val downloadError = (updateDownloadState as? UpdateDownloadState.Error)?.message
        val isDownloading = updateDownloadState is UpdateDownloadState.Downloading
        AlertDialog(
            onDismissRequest = {
                if (!isDownloading) updateDismissed = true
            },
            title = { Text(stringResource(Res.string.update_available), color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column {
                    Text(
                        text = "${stringResource(Res.string.update_available_message)} ${latest.tagName}",
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    when {
                        isDownloading -> {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(stringResource(Res.string.update_downloading), color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        downloadError != null -> {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "${stringResource(Res.string.update_download_error)} $downloadError",
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        updateDownloadState is UpdateDownloadState.Launched -> {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(stringResource(Res.string.update_launched), color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.downloadAndInstallUpdate(latest) },
                    enabled = !isDownloading,
                ) {
                    Text(
                        text = if (downloadError != null) stringResource(Res.string.retry)
                        else stringResource(Res.string.update_download),
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.resetUpdateDownloadState()
                        updateDismissed = true
                    },
                ) {
                    Text(stringResource(Res.string.update_dismiss))
                }
            },
        )
    }
}
