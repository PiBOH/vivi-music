package com.music.vivi.desktop

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Desktop login. The preferred flow opens YouTube Music in the optional embedded
 * WebView; if that runtime is unavailable, it opens the system browser and keeps
 * the existing cookie fallback available. [onLoggedIn] is invoked on success.
 */
@Composable
fun LoginScreen(language: String, onBack: () -> Unit, onLoggedIn: () -> Unit) {
    var cookie by remember { mutableStateOf("") }
    var dataSyncId by remember { mutableStateOf("") }
    var visitorData by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var loginMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
    ) {
        BackButton(language, onBack)
        Text(Localization.get(language, "login"), style = MaterialTheme.typography.headlineMedium)
        Card(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text(Localization.get(language, "login_webview_title"), style = MaterialTheme.typography.titleMedium)
                Text(
                    Localization.get(language, "login_webview_desc"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Button(
                    onClick = {
                        loginMessage = if (LoginWebView.openEmbedded()) {
                            Localization.get(language, "login_webview_opened")
                        } else if (LoginWebView.openBrowser()) {
                            Localization.get(language, "login_browser_opened")
                        } else {
                            Localization.get(language, "login_browser_failed")
                        }
                    },
                    modifier = Modifier.padding(top = 12.dp),
                ) { Text(Localization.get(language, "login_with_webview")) }
                loginMessage?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
        Text(
            Localization.get(language, "login_manual_fallback"),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 20.dp),
        )
        Text(
            Localization.get(language, "login_instructions"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
        OutlinedTextField(
            value = cookie,
            onValueChange = { cookie = it },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(160.dp),
            label = { Text(Localization.get(language, "cookie_label")) },
        )

        // Optional manual fallbacks, used only when auto-detection fails.
        Text(
            Localization.get(language, "advanced_login_hint"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp),
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
                    loading = true
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
                        onLoggedIn()
                    } catch (e: Exception) {
                        error = e.message ?: (e::class.simpleName ?: "error")
                    } finally {
                        loading = false
                    }
                }
            },
            enabled = cookie.isNotBlank() && !loading,
            modifier = Modifier.padding(top = 16.dp),
        ) {
            Text(if (loading) Localization.get(language, "logging_in") else Localization.get(language, "login"))
        }

        status?.let {
            Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
        }
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }
    }
}
