package com.music.vivi.desktop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Desktop YouTube sign-in — Material 3, two clear paths:
 *
 *  1. **Sign in with Google** (primary): opens the embedded sign-in window
 *     immediately. The window loads the Google sign-in page directly, the
 *     session cookies are captured automatically when Google redirects back to
 *     YouTube Music, and the window closes by itself (see [LoginWebView]).
 *  2. **Manual cookies** (secondary, collapsed): paste the Cookie header from
 *     the browser — kept for systems where the embedded window can't run.
 *
 * [onLoggedIn] is invoked after a successful, validated login.
 */
@Composable
fun LoginScreen(language: String, onBack: () -> Unit, onLoggedIn: () -> Unit) {
    val scope = rememberCoroutineScope()

    // Signed-in state (restored from settings on entry).
    var accountName by remember { mutableStateOf(DesktopSettings.load().accountName) }
    var isLoggedIn by remember { mutableStateOf(LoginManager.isLoggedIn()) }

    var manualOpen by remember { mutableStateOf(false) }
    var cookie by remember { mutableStateOf("") }
    var dataSyncId by remember { mutableStateOf("") }
    var visitorData by remember { mutableStateOf("") }

    var status by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var savingCookie by remember { mutableStateOf(false) }
    var waitingForWindow by remember { mutableStateOf(false) }

    fun refreshAccount() {
        isLoggedIn = LoginManager.isLoggedIn()
        accountName = DesktopSettings.load().accountName
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
    ) {
        BackButton(language, onBack)
        Text(Localization.get(language, "login"), style = MaterialTheme.typography.headlineMedium)

        // ------------------------------------------------ signed-in state
        if (isLoggedIn) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(14.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${Localization.get(language, "logged_in_as")}: $accountName",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            Localization.get(language, "login_signed_in_hint"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    TextButton(onClick = {
                        scope.launch(Dispatchers.IO) {
                            LoginManager.logout()
                            refreshAccount()
                        }
                    }) { Text(Localization.get(language, "logout")) }
                }
            }
        }

        // ------------------------------------------------ path 1: Google window
        Card(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    Localization.get(language, "login_google"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                listOf("login_step1", "login_step2", "login_step3").forEach { key ->
                    Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.Top) {
                        Text("• ", color = MaterialTheme.colorScheme.primary)
                        Text(
                            Localization.get(language, key),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Button(
                    onClick = {
                        error = null
                        status = null
                        waitingForWindow = true
                        val opened = LoginWebView.openEmbedded(language) { captured ->
                            waitingForWindow = false
                            if (captured != null) {
                                // Auto-captured session: validate + persist exactly
                                // like the manual flow, then hand back to the app.
                                scope.launch {
                                    savingCookie = true
                                    try {
                                        val account = withContext(Dispatchers.IO) {
                                            LoginManager.login(cookie = captured)
                                        }
                                        status = "${Localization.get(language, "logged_in_as")}: ${account.name}"
                                        refreshAccount()
                                        onLoggedIn()
                                    } catch (e: Exception) {
                                        // Keep the captured cookies in the manual field
                                        // so a retry is a single click.
                                        cookie = captured
                                        manualOpen = true
                                        error = e.message ?: (e::class.simpleName ?: "error")
                                    } finally {
                                        savingCookie = false
                                    }
                                }
                            } else if (!LoginManager.isLoggedIn()) {
                                status = Localization.get(language, "login_window_closed")
                            }
                        }
                        if (!opened) {
                            waitingForWindow = false
                            error = Localization.get(language, "login_webview_unavailable")
                        }
                    },
                    enabled = !waitingForWindow && !savingCookie,
                    modifier = Modifier.padding(top = 12.dp),
                ) {
                    Text(
                        when {
                            savingCookie -> Localization.get(language, "login_saving")
                            waitingForWindow -> Localization.get(language, "login_waiting")
                            else -> Localization.get(language, "login_google")
                        },
                    )
                }
            }
        }

        status?.let {
            Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
        }
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }

        // Embedded window unavailable → offer the browser fallback right here.
        if (error == Localization.get(language, "login_webview_unavailable")) {
            OutlinedButton(
                onClick = { LoginWebView.openBrowser() },
                modifier = Modifier.padding(top = 4.dp),
            ) {
                Text(Localization.get(language, "login_open_browser"))
            }
        }

        // ------------------------------------------------ path 2: manual cookies
        Card(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    Modifier.fillMaxWidth().clickable { manualOpen = !manualOpen },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        Localization.get(language, "login_manual_title"),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        Localization.get(language, if (manualOpen) "login_hide" else "login_show"),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }

                AnimatedVisibility(visible = manualOpen) {
                    Column {
                        Text(
                            Localization.get(language, "login_instructions"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 10.dp),
                        )
                        OutlinedTextField(
                            value = cookie,
                            onValueChange = { cookie = it },
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(140.dp),
                            label = { Text(Localization.get(language, "cookie_label")) },
                        )

                        // Optional manual fallbacks, used only when the automatic
                        // extraction from the music.youtube.com shell fails.
                        Text(
                            Localization.get(language, "advanced_login_hint"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                        OutlinedTextField(
                            value = dataSyncId,
                            onValueChange = { dataSyncId = it },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            label = { Text(Localization.get(language, "data_sync_id_label")) },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = visitorData,
                            onValueChange = { visitorData = it },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            label = { Text(Localization.get(language, "visitor_data_label")) },
                            singleLine = true,
                        )

                        Button(
                            onClick = {
                                scope.launch {
                                    savingCookie = true
                                    error = null
                                    status = null
                                    try {
                                        val account = withContext(Dispatchers.IO) {
                                            LoginManager.login(
                                                cookie = cookie,
                                                dataSyncIdOverride = dataSyncId.ifBlank { null },
                                                visitorDataOverride = visitorData.ifBlank { null },
                                            )
                                        }
                                        status = "${Localization.get(language, "logged_in_as")}: ${account.name}"
                                        refreshAccount()
                                        onLoggedIn()
                                    } catch (e: Exception) {
                                        error = e.message ?: (e::class.simpleName ?: "error")
                                    } finally {
                                        savingCookie = false
                                    }
                                }
                            },
                            enabled = cookie.isNotBlank() && !savingCookie,
                            modifier = Modifier.padding(top = 16.dp),
                        ) {
                            Text(if (savingCookie) Localization.get(language, "logging_in") else Localization.get(language, "login"))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}
