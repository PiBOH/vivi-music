package com.vivimusic.de.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.vivimusic.de.data.sync.SyncStatus
import com.vivimusic.de.domain.LibraryItem
import com.vivimusic.de.resources.*
import com.vivimusic.de.ui.AppViewModel
import com.vivimusic.de.ui.AuthState
import com.vivimusic.de.ui.copyToClipboard
import com.vivimusic.de.ui.AxolotlMascot
import com.vivimusic.de.ui.ChipsRow
import com.vivimusic.de.ui.screens.settings.SettingsDivider
import com.vivimusic.de.ui.screens.settings.SettingsGroup
import com.vivimusic.de.ui.screens.settings.SettingsItem
import org.jetbrains.compose.resources.stringResource

private enum class AccountPage { YtLogin, SupabaseLogin, ActivityHistory }

private enum class AccountLibraryFilter { Playlists, Albums, Artists }

/**
 * Account screen, ported from ViVi Music's `AccountScreen` and adapted for the
 * desktop: the signed-in YouTube Music account (profile + liked library) plus
 * the Supabase data-synchronization section and the listening activity.
 */
@Composable
fun AccountScreen(
    viewModel: AppViewModel,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
) {
    var page by remember { mutableStateOf<AccountPage?>(null) }

    when (page) {
        null -> AccountMain(
            viewModel = viewModel,
            onLoginYt = { page = AccountPage.YtLogin },
            onLoginSupabase = { page = AccountPage.SupabaseLogin },
            onActivityHistory = { page = AccountPage.ActivityHistory },
            onOpenAlbum = onOpenAlbum,
            onOpenArtist = onOpenArtist,
        )
        AccountPage.YtLogin -> YtLoginScreen(viewModel, onBack = { page = null })
        AccountPage.SupabaseLogin -> LoginScreen(viewModel, onBack = { page = null })
        AccountPage.ActivityHistory -> ActivityHistoryScreen(viewModel, onBack = { page = null })
    }
}

@Composable
private fun AccountMain(
    viewModel: AppViewModel,
    onLoginYt: () -> Unit,
    onLoginSupabase: () -> Unit,
    onActivityHistory: () -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
) {
    val info by viewModel.ytAccountInfo.collectAsState()
    val signedIn by viewModel.ytSignedIn.collectAsState()
    val library by viewModel.ytLibrary.collectAsState()
    val libraryLoading by viewModel.ytLibraryLoading.collectAsState()
    val authState by viewModel.authState.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    var filter by remember { mutableStateOf(AccountLibraryFilter.Playlists) }

    val filteredLibrary = library.filter { item ->
        when (filter) {
            AccountLibraryFilter.Playlists -> item is LibraryItem.Playlist
            AccountLibraryFilter.Albums -> item is LibraryItem.Album
            AccountLibraryFilter.Artists -> item is LibraryItem.Artist
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                text = stringResource(Res.string.account),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }

        // ----- YouTube Music account -----
        item(span = { GridItemSpan(maxLineSpan) }) {
            val currentInfo = info
            if (signedIn && currentInfo != null) {
                YtProfileHeader(currentInfo, onSignOut = viewModel::signOutYt)
            } else {
                YtSignInCard(onLogin = onLoginYt)
            }
        }

        if (signedIn) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                val playlistsLabel = stringResource(Res.string.filter_playlists)
                val albumsLabel = stringResource(Res.string.filter_albums)
                val artistsLabel = stringResource(Res.string.filter_artists)
                ChipsRow(
                    chips = listOf(
                        AccountLibraryFilter.Playlists to playlistsLabel,
                        AccountLibraryFilter.Albums to albumsLabel,
                        AccountLibraryFilter.Artists to artistsLabel,
                    ),
                    currentValue = filter,
                    onValueUpdate = { filter = it },
                )
            }

            items(filteredLibrary, key = { it.id }) { item ->
                LibraryGridItem(
                    item = item,
                    onClick = {
                        when (item) {
                            is LibraryItem.Artist -> onOpenArtist(item.id)
                            else -> onOpenAlbum(item.id)
                        }
                    },
                )
            }

            if (libraryLoading && filteredLibrary.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }

        // ----- data synchronization (Supabase) + activity -----
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                SettingsGroup(title = stringResource(Res.string.account_data_sync)) {
                    when (val state = authState) {
                        is AuthState.SignedIn -> {
                            SettingsItem(
                                title = state.email,
                                description = stringResource(Res.string.signed_in_as),
                                icon = Icons.Filled.Person,
                            )
                            SettingsDivider()
                            SettingsItem(
                                title = stringResource(Res.string.sync_status),
                                trailingText = syncStatusLabel(syncStatus),
                            )
                            SettingsDivider()
                            SettingsItem(
                                title = stringResource(Res.string.sign_out),
                                icon = Icons.AutoMirrored.Filled.Logout,
                                onClick = viewModel::signOut,
                            )
                        }
                        is AuthState.Checking -> SettingsItem(
                            title = stringResource(Res.string.sync_status),
                            trailingText = stringResource(Res.string.sync_syncing),
                        )
                        else -> SettingsItem(
                            title = stringResource(Res.string.sign_in),
                            description = stringResource(Res.string.privacy_sync_desc),
                            icon = Icons.Filled.Person,
                            onClick = onLoginSupabase,
                        )
                    }
                }

                SettingsGroup {
                    SettingsItem(
                        title = stringResource(Res.string.activity_history),
                        icon = Icons.Filled.History,
                        onClick = onActivityHistory,
                    )
                }
            }
        }
    }
}

@Composable
private fun YtProfileHeader(info: com.vivimusic.de.domain.AccountInfo, onSignOut: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    ) {
        if (!info.thumbnailUrl.isNullOrBlank()) {
            AsyncImage(
                model = info.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(96.dp).clip(CircleShape),
            )
        } else {
            AxolotlMascot(modifier = Modifier.size(96.dp))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = info.name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        val secondary = info.email?.takeIf { it.isNotBlank() } ?: info.channelHandle.orEmpty()
        if (secondary.isNotBlank()) {
            Text(
                text = secondary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onSignOut) {
            Text(stringResource(Res.string.sign_out))
        }
    }
}

@Composable
private fun YtSignInCard(onLogin: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(20.dp),
    ) {
        AxolotlMascot(modifier = Modifier.size(80.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(Res.string.account_sign_in_ytm),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(Res.string.account_ytm_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onLogin) {
            Text(stringResource(Res.string.sign_in))
        }
    }
}

@Composable
private fun LibraryGridItem(item: LibraryItem, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            if (!item.thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = item.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(32.dp).align(Alignment.Center),
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = item.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (item.subtitle.isNotBlank()) {
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun YtLoginScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    var cookie by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
            }
            Text(
                text = stringResource(Res.string.account_sign_in_ytm),
                style = MaterialTheme.typography.headlineSmall,
            )
        }

        Text(
            text = stringResource(Res.string.account_ytm_cookie_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 16.dp),
        )

        OutlinedTextField(
            value = cookie,
            onValueChange = { cookie = it },
            label = { Text(stringResource(Res.string.account_ytm_cookie_label)) },
            placeholder = { Text(stringResource(Res.string.account_ytm_cookie_hint)) },
            enabled = !busy,
            minLines = 5,
            modifier = Modifier.fillMaxWidth(),
        )

        if (error != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = error.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
            TextButton(onClick = { copyToClipboard(error.orEmpty()) }) {
                Text(stringResource(Res.string.copy_error))
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                busy = true
                error = null
                viewModel.signInYt(cookie.trim()) { result ->
                    busy = false
                    error = result
                    if (result == null) onBack()
                }
            },
            enabled = !busy && cookie.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
            if (busy) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text(stringResource(Res.string.sign_in))
            }
        }
    }
}

@Composable
private fun syncStatusLabel(status: SyncStatus): String = when (status) {
    SyncStatus.Disabled -> stringResource(Res.string.sync_disabled)
    SyncStatus.Offline -> stringResource(Res.string.sync_offline)
    SyncStatus.Syncing -> stringResource(Res.string.sync_syncing)
    SyncStatus.Synced -> stringResource(Res.string.sync_synced)
    SyncStatus.Error -> stringResource(Res.string.sync_error)
}
