package com.music.vivi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Parses a YouTube watch URL / youtu.be link / bare video id into a video id. */
private fun extractVideoId(input: String): String {
    val raw = input.trim()
    if (raw.isEmpty()) return ""
    val watch = Regex("(?:youtube\\.com|youtu\\.be)/(?:watch\\?v=|shorts/|embed/|live/)?([\\w-]{11})").find(raw)
    if (watch != null) return watch.groupValues[1]
    return if (raw.length == 11) raw else ""
}

@Composable
fun ListenTogetherScreen(
    language: String,
    onBack: () -> Unit,
    manager: ListenTogetherManager,
) {
    val connectionState by manager.connectionState.collectAsState()
    val roomState by manager.roomState.collectAsState()
    val role by manager.role.collectAsState()
    val userId by manager.userId.collectAsState()
    val pendingJoin by manager.pendingJoinRequests.collectAsState()
    val buffering by manager.bufferingUsers.collectAsState()
    val pendingSuggestions by manager.pendingSuggestions.collectAsState()
    val messages by manager.chatMessages.collectAsState()
    val busy by manager.busy.collectAsState()

    var usernameInput by remember { mutableStateOf(DesktopSettings.load().listenTogetherUsername) }
    var roomCodeInput by remember { mutableStateOf("") }
    var serverInput by remember { mutableStateOf(DesktopSettings.load().listenTogetherServerUrl) }
    var autoApprove by remember { mutableStateOf(DesktopSettings.load().listenTogetherAutoApproval) }
    var syncVolume by remember { mutableStateOf(DesktopSettings.load().listenTogetherSyncVolume) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var chatInput by remember { mutableStateOf("") }
    var suggestInput by remember { mutableStateOf("") }
    var copied by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(manager) {
        manager.events.collect { e ->
            when (e) {
                is LtEvent.Error -> error = e.message
                is LtEvent.JoinRejected -> error = e.reason
                is LtEvent.Kicked -> error = Localization.get(language, "lt_kicked") + (if (e.reason.isNotBlank()) ": ${e.reason}" else "")
                is LtEvent.RoomCreated, is LtEvent.JoinApproved, is LtEvent.Reconnected -> {
                    error = null
                    notice = null
                }
                is LtEvent.SuggestionApproved -> notice = Localization.get(language, "lt_suggestion_approved")
                is LtEvent.SuggestionRejected -> notice = Localization.get(language, "lt_suggestion_rejected")
                is LtEvent.Chat -> Unit
                else -> Unit
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BackButton(language, onBack)
            Spacer(Modifier.width(8.dp))
            Text(Localization.get(language, "listen_together"), style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.weight(1f))
            ConnectionBadge(connectionState, language)
        }
        Spacer(Modifier.height(8.dp))

        if (roomState == null) {
            Lobby(
                language = language,
                username = usernameInput,
                onUsername = { usernameInput = it },
                roomCode = roomCodeInput,
                onRoomCode = { if (it.length <= 8) roomCodeInput = it.uppercase() },
                server = serverInput,
                onServer = { serverInput = it },
                autoApprove = autoApprove,
                onAutoApprove = {
                    autoApprove = it
                    DesktopSettings.update { s -> s.copy(listenTogetherAutoApproval = it) }
                },
                busy = busy,
                error = error,
                onCreate = {
                    error = null
                    DesktopSettings.update { s -> s.copy(listenTogetherUsername = usernameInput.trim()) }
                    manager.createRoom(usernameInput)
                },
                onJoin = {
                    error = null
                    DesktopSettings.update { s -> s.copy(listenTogetherUsername = usernameInput.trim()) }
                    manager.joinRoom(roomCodeInput, usernameInput)
                },
            )
        } else {
            val r = roomState!!
            val isHost = r.hostId == userId
            InRoom(
                language = language,
                room = r,
                isHost = isHost,
                myUserId = userId,
                pendingJoin = pendingJoin,
                buffering = buffering,
                pendingSuggestions = pendingSuggestions,
                messages = messages,
                busy = busy,
                error = error,
                notice = notice,
                chatInput = chatInput,
                onChatInput = { chatInput = it },
                suggestInput = suggestInput,
                onSuggestInput = { suggestInput = it },
                autoApprove = autoApprove,
                onAutoApprove = {
                    autoApprove = it
                    DesktopSettings.update { s -> s.copy(listenTogetherAutoApproval = it) }
                },
                syncVolume = syncVolume,
                onSyncVolume = {
                    syncVolume = it
                    DesktopSettings.update { s -> s.copy(listenTogetherSyncVolume = it) }
                },
                copied = copied,
                onCopy = {
                    val cb = java.awt.Toolkit.getDefaultToolkit().systemClipboard
                    cb.setContents(java.awt.datatransfer.StringSelection(r.roomCode), null)
                    copied = true
                    scope.launch { kotlinx.coroutines.delay(1500); copied = false }
                },
                onApproveJoin = { manager.approveJoin(it) },
                onRejectJoin = { manager.rejectJoin(it) },
                onKick = { manager.kickUser(it) },
                onTransferHost = { manager.transferHost(it) },
                onBlock = { manager.blockUser(it) },
                onSendChat = {
                    manager.sendChatMessage(chatInput)
                    chatInput = ""
                },
                onRequestSync = { manager.requestSync() },
                onSuggest = {
                    val vid = extractVideoId(suggestInput)
                    if (vid.isNotEmpty()) {
                        scope.launch(Dispatchers.IO) {
                            YouTube.queue(listOf(vid)).onSuccess { q ->
                                val song = q.firstOrNull()
                                if (song != null) {
                                    manager.suggestTrack(
                                        LtTrackInfo(
                                            id = song.id,
                                            title = song.title,
                                            artist = song.artists.joinToString(", ") { it.name },
                                            duration = (song.duration ?: 0) * 1000L,
                                            thumbnail = song.thumbnail,
                                        )
                                    )
                                } else {
                                    manager.suggestTrack(LtTrackInfo(id = vid, title = vid, artist = ""))
                                }
                            }.onFailure {
                                manager.suggestTrack(LtTrackInfo(id = vid, title = vid, artist = ""))
                            }
                        }
                        suggestInput = ""
                    }
                },
                onApproveSuggestion = { manager.approveSuggestion(it) },
                onRejectSuggestion = { manager.rejectSuggestion(it) },
                onLeave = { manager.leaveRoom() },
                onReconnect = { manager.forceReconnect() },
            )
        }
    }
}

@Composable
private fun ConnectionBadge(state: LtConnectionState, language: String) {
    val (label, color) = when (state) {
        LtConnectionState.CONNECTED -> Localization.get(language, "connected") to MaterialTheme.colorScheme.primary
        LtConnectionState.CONNECTING -> Localization.get(language, "lt_connecting") to MaterialTheme.colorScheme.tertiary
        LtConnectionState.RECONNECTING -> Localization.get(language, "lt_reconnecting") to MaterialTheme.colorScheme.tertiary
        LtConnectionState.DISCONNECTED, LtConnectionState.ERROR -> Localization.get(language, "disconnected") to MaterialTheme.colorScheme.error
    }
    Card(colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f))) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(Modifier.size(8.dp).background(color, CircleShape))
            Text(label, style = MaterialTheme.typography.labelMedium, color = color)
        }
    }
}

@Composable
private fun Lobby(
    language: String,
    username: String,
    onUsername: (String) -> Unit,
    roomCode: String,
    onRoomCode: (String) -> Unit,
    server: String,
    onServer: (String) -> Unit,
    autoApprove: Boolean,
    onAutoApprove: (Boolean) -> Unit,
    busy: Boolean,
    error: String?,
    onCreate: () -> Unit,
    onJoin: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                Localization.get(language, "listen_together_description"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = username,
                onValueChange = onUsername,
                label = { Text(Localization.get(language, "username")) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = roomCode,
                onValueChange = onRoomCode,
                label = { Text(Localization.get(language, "room_code")) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = server,
                onValueChange = onServer,
                label = { Text(Localization.get(language, "relay_server")) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    Localization.get(language, "lt_auto_approve"),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = autoApprove, onCheckedChange = onAutoApprove)
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    enabled = username.isNotBlank() && !busy,
                    onClick = onCreate,
                ) {
                    if (busy) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Text(Localization.get(language, "create"))
                }
                OutlinedButton(
                    enabled = username.isNotBlank() && roomCode.isNotBlank() && !busy,
                    onClick = onJoin,
                ) { Text(Localization.get(language, "connect")) }
            }
        }
    }
}

@Composable
private fun InRoom(
    language: String,
    room: LtRoomState,
    isHost: Boolean,
    myUserId: String?,
    pendingJoin: List<LtJoinRequest>,
    buffering: List<String>,
    pendingSuggestions: List<LtSuggestionReceived>,
    messages: List<LtChatMessage>,
    busy: Boolean,
    error: String?,
    notice: String?,
    chatInput: String,
    onChatInput: (String) -> Unit,
    suggestInput: String,
    onSuggestInput: (String) -> Unit,
    autoApprove: Boolean,
    onAutoApprove: (Boolean) -> Unit,
    syncVolume: Boolean,
    onSyncVolume: (Boolean) -> Unit,
    copied: Boolean,
    onCopy: () -> Unit,
    onApproveJoin: (String) -> Unit,
    onRejectJoin: (String) -> Unit,
    onKick: (String) -> Unit,
    onTransferHost: (String) -> Unit,
    onBlock: (String) -> Unit,
    onSendChat: () -> Unit,
    onRequestSync: () -> Unit,
    onSuggest: () -> Unit,
    onApproveSuggestion: (String) -> Unit,
    onRejectSuggestion: (String) -> Unit,
    onLeave: () -> Unit,
    onReconnect: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(Localization.get(language, "room_code"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    room.roomCode,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                TextButton(onClick = onCopy) { Text(if (copied) Localization.get(language, "copied_to_clipboard") else Localization.get(language, "lt_copy_code")) }
            }
            Text(
                Localization.get(language, "connected_users") + " (${room.users.size})",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            if (buffering.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CircularProgressIndicator(Modifier.size(12.dp), strokeWidth = 2.dp)
                    Text(
                        Localization.get(language, "lt_buffering") + " (${buffering.size})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }

    Spacer(Modifier.height(12.dp))
    error?.let {
        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(8.dp))
    }
    notice?.let {
        Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(8.dp))
    }

    LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
        // --- Users ---
        item(key = "users_header") {
            Text(
                Localization.get(language, "connected_users"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))
        }
        items(room.users, key = { it.userId }) { user ->
            val isMe = user.userId == myUserId
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    user.username + if (isMe) " (${Localization.get(language, "lt_you")})" else "",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!user.isConnected) {
                    Text("(${Localization.get(language, "disconnected")})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (user.isHost) {
                    Text("👑", style = MaterialTheme.typography.bodyMedium)
                } else if (isHost) {
                    TextButton(onClick = { onTransferHost(user.userId) }) { Text(Localization.get(language, "lt_transfer_host"), style = MaterialTheme.typography.labelSmall) }
                    IconButton(onClick = { onKick(user.userId) }) {
                        Icon(Icons.Filled.Close, contentDescription = Localization.get(language, "lt_kick"), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // --- Join requests (host) ---
        if (isHost && pendingJoin.isNotEmpty()) {
            item(key = "joins_header") {
                Spacer(Modifier.height(8.dp))
                Text(Localization.get(language, "lt_join_requests"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            items(pendingJoin, key = { "req-${it.userId}" }) { req ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${req.username} " + Localization.get(language, "connect"), modifier = Modifier.weight(1f))
                    TextButton(onClick = { onApproveJoin(req.userId) }) { Text("✓") }
                    TextButton(onClick = { onRejectJoin(req.userId) }) { Text("✕") }
                }
            }
        }

        // --- Suggestions ---
        item(key = "suggest_header") {
            Spacer(Modifier.height(8.dp))
            Text(Localization.get(language, "suggestions"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
        }
        if (isHost) {
            if (pendingSuggestions.isEmpty()) {
                item(key = "no_suggestions") {
                    Text(Localization.get(language, "lt_no_suggestions"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(pendingSuggestions, key = { it.suggestionId }) { s ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(s.trackInfo.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "${s.fromUsername} · ${s.trackInfo.artist}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        TextButton(onClick = { onApproveSuggestion(s.suggestionId) }) { Text("✓") }
                        TextButton(onClick = { onRejectSuggestion(s.suggestionId) }) { Text("✕") }
                    }
                }
            }
        } else {
            item(key = "suggest_input") {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = suggestInput,
                        onValueChange = onSuggestInput,
                        placeholder = { Text(Localization.get(language, "lt_suggest_placeholder")) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = onSuggest, enabled = suggestInput.isNotBlank()) { Text(Localization.get(language, "lt_suggest")) }
                }
            }
        }

        // --- Options ---
        item(key = "options") {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(Localization.get(language, "lt_auto_approve"), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                Switch(checked = autoApprove, onCheckedChange = onAutoApprove)
            }
            if (isHost) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(Localization.get(language, "lt_sync_volume"), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    Switch(checked = syncVolume, onCheckedChange = onSyncVolume)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onRequestSync) { Text(Localization.get(language, "lt_request_sync")) }
                OutlinedButton(onClick = onReconnect) { Text(Localization.get(language, "lt_reconnect")) }
                Button(onClick = onLeave) { Text(Localization.get(language, "leave_room")) }
            }
        }

        // --- Chat ---
        item(key = "chat_header") {
            Spacer(Modifier.height(12.dp))
            Text(Localization.get(language, "comments"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        items(messages, key = { "${it.timestamp}-${it.userId}-${it.message}" }) { m ->
            Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(
                    m.username,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                m.replyTo?.let { reply ->
                    Text(
                        "↪ ${reply.username}: ${reply.message}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(m.message, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = chatInput,
            onValueChange = onChatInput,
            placeholder = { Text(Localization.get(language, "comments")) },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Button(onClick = onSendChat) { Text("➤") }
    }
    }
}