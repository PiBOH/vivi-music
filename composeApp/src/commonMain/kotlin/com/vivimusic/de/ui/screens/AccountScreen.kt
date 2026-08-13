package com.vivimusic.de.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vivimusic.de.data.sync.SyncStatus
import com.vivimusic.de.resources.*
import com.vivimusic.de.ui.AppViewModel
import com.vivimusic.de.ui.AuthState
import com.vivimusic.de.ui.AxolotlMascot
import com.vivimusic.de.ui.screens.settings.SettingsDivider
import com.vivimusic.de.ui.screens.settings.SettingsGroup
import com.vivimusic.de.ui.screens.settings.SettingsItem
import com.vivimusic.de.ui.screens.settings.SettingsPage
import org.jetbrains.compose.resources.stringResource

private enum class AccountPage { Login, ActivityHistory }

/**
 * Account screen, ported from ViVi Music's account area and adapted for the
 * desktop: the Supabase account (used for data sync) with sign-in, sign-out,
 * sync status and the listening activity history.
 */
@Composable
fun AccountScreen(viewModel: AppViewModel) {
    var page by remember { mutableStateOf<AccountPage?>(null) }

    when (page) {
        null -> AccountMain(
            viewModel = viewModel,
            onLogin = { page = AccountPage.Login },
            onActivityHistory = { page = AccountPage.ActivityHistory },
        )
        AccountPage.Login -> LoginScreen(viewModel, onBack = { page = null })
        AccountPage.ActivityHistory -> ActivityHistoryScreen(viewModel, onBack = { page = null })
    }
}

@Composable
private fun AccountMain(
    viewModel: AppViewModel,
    onLogin: () -> Unit,
    onActivityHistory: () -> Unit,
) {
    val authState by viewModel.authState.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()

    SettingsPage(title = stringResource(Res.string.account)) {
        when (val state = authState) {
            is AuthState.Checking -> Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            is AuthState.SignedIn -> {
                AccountHeader(
                    email = state.email,
                    subtitle = stringResource(Res.string.signed_in_as),
                )
                SettingsGroup {
                    SettingsItem(
                        title = stringResource(Res.string.activity_history),
                        icon = Icons.Filled.History,
                        onClick = onActivityHistory,
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
            }

            else -> {
                AccountHeader(
                    email = null,
                    subtitle = stringResource(Res.string.not_signed_in),
                )
                if (state is AuthState.Error) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    )
                }
                SettingsGroup {
                    SettingsItem(
                        title = stringResource(Res.string.sign_in),
                        icon = Icons.Filled.Person,
                        onClick = onLogin,
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountHeader(email: String?, subtitle: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
    ) {
        AxolotlMascot(modifier = Modifier.size(96.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = email ?: stringResource(Res.string.not_signed_in),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
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
