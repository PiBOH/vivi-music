package com.music.vivi.desktop

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.music.innertube.models.SongItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

// ---------------------------------------------------------------------------
// Listen Together wire protocol (JSON subset of the mobile `Protocol.kt`).
// ---------------------------------------------------------------------------

@Serializable
data class LtMessage(val type: String, val payload: JsonElement? = null)

@Serializable
data class LtCreateRoom(val username: String)

@Serializable
data class LtJoinRoom(@SerialName("room_code") val roomCode: String, val username: String)

@Serializable
data class LtApproveJoin(@SerialName("user_id") val userId: String)

@Serializable
data class LtRejectJoin(@SerialName("user_id") val userId: String, val reason: String? = null)

@Serializable
data class LtChat(val message: String)

@Serializable
data class LtUser(
    @SerialName("user_id") val userId: String,
    val username: String,
    @SerialName("is_host") val isHost: Boolean = false,
    @SerialName("is_connected") val isConnected: Boolean = true,
)

@Serializable
data class LtTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val duration: Long = 0L,
    val thumbnail: String? = null,
)

@Serializable
data class LtRoomState(
    @SerialName("room_code") val roomCode: String,
    @SerialName("host_id") val hostId: String,
    val users: List<LtUser>,
    @SerialName("current_track") val currentTrack: LtTrack? = null,
    @SerialName("is_playing") val isPlaying: Boolean = false,
    val position: Long = 0L,
    val queue: List<LtTrack> = emptyList(),
)

@Serializable
data class LtRoomCreated(
    @SerialName("room_code") val roomCode: String,
    @SerialName("user_id") val userId: String,
    @SerialName("session_token") val sessionToken: String,
)

@Serializable
data class LtJoinApproved(
    @SerialName("room_code") val roomCode: String,
    @SerialName("user_id") val userId: String,
    @SerialName("session_token") val sessionToken: String,
    val state: LtRoomState,
)

@Serializable
data class LtJoinRequest(@SerialName("user_id") val userId: String, val username: String)

@Serializable
data class LtJoinRejected(val reason: String)

@Serializable
data class LtUserJoined(@SerialName("user_id") val userId: String, val username: String)

@Serializable
data class LtUserLeft(@SerialName("user_id") val userId: String, val username: String)

@Serializable
data class LtChatMessage(
    @SerialName("user_id") val userId: String,
    val username: String,
    val message: String,
    val timestamp: Long,
)

@Serializable
data class LtPlayback(
    val action: String,
    @SerialName("track_id") val trackId: String? = null,
    val position: Long? = null,
    @SerialName("track_info") val trackInfo: LtTrack? = null,
)

@Serializable
data class LtError(val code: String, val message: String)

@Serializable
data class LtSyncState(
    @SerialName("current_track") val currentTrack: LtTrack? = null,
    @SerialName("is_playing") val isPlaying: Boolean = false,
    val position: Long = 0L,
)

sealed class LtEvent {
    data object Connected : LtEvent()
    data object Disconnected : LtEvent()
    data class RoomCreated(val roomCode: String, val userId: String) : LtEvent()
    data class Joined(val roomCode: String, val userId: String, val state: LtRoomState) : LtEvent()
    data class JoinRequest(val userId: String, val username: String) : LtEvent()
    data class UserJoined(val userId: String, val username: String) : LtEvent()
    data class UserLeft(val userId: String, val username: String) : LtEvent()
    data class Chat(val message: LtChatMessage) : LtEvent()
    data class Playback(val playback: LtPlayback) : LtEvent()
    data class SyncState(val state: LtSyncState) : LtEvent()
    data class Error(val message: String) : LtEvent()
}

/**
 * Minimal desktop Listen Together client. Reuses the mobile JSON protocol
 * (`{type, payload}`) so it can talk to the same public relay servers.
 */
class ListenTogetherClient {
    companion object {
        const val DEFAULT_URL = "wss://vivimusic-listen-together.onrender.com"
    }

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; isLenient = true }
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .pingInterval(25, TimeUnit.SECONDS)
        .build()

    private val _events = MutableSharedFlow<LtEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<LtEvent> = _events.asSharedFlow()

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private val _room = MutableStateFlow<LtRoomState?>(null)
    val room: StateFlow<LtRoomState?> = _room.asStateFlow()

    private val _userId = MutableStateFlow<String?>(null)
    val userId: StateFlow<String?> = _userId.asStateFlow()

    private val _pendingJoin = MutableStateFlow<List<LtJoinRequest>>(emptyList())
    val pendingJoin: StateFlow<List<LtJoinRequest>> = _pendingJoin.asStateFlow()

    private val _messages = MutableStateFlow<List<LtChatMessage>>(emptyList())
    val messages: StateFlow<List<LtChatMessage>> = _messages.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private var socket: WebSocket? = null
    private var username = ""
    private var pendingAction: (() -> Unit)? = null

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            _connected.value = true
            pendingAction?.invoke()
            pendingAction = null
            scope.launch { _events.emit(LtEvent.Connected) }
        }

        override fun onMessage(webSocket: WebSocket, text: String) = handle(text)

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            _connected.value = false
            _room.value = null
            _userId.value = null
            _pendingJoin.value = emptyList()
            scope.launch { _events.emit(LtEvent.Disconnected) }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            _connected.value = false
            scope.launch { _events.emit(LtEvent.Error(t.message ?: "Connection failed")) }
        }
    }

    fun connect() {
        if (socket != null && _connected.value) return
        _connected.value = false
        socket = http.newWebSocket(Request.Builder().url(DEFAULT_URL).build(), listener)
    }

    fun disconnect() {
        socket?.close(1000, "bye")
        socket = null
        _connected.value = false
        _room.value = null
        _userId.value = null
        _pendingJoin.value = emptyList()
        _messages.value = emptyList()
    }

    fun createRoom(name: String) {
        username = name.trim()
        _busy.value = true
        send(LtMessage("create_room", json.encodeToJsonElement(LtCreateRoom.serializer(), LtCreateRoom(username))))
    }

    fun joinRoom(code: String, name: String) {
        username = name.trim()
        _busy.value = true
        send(LtMessage("join_room", json.encodeToJsonElement(LtJoinRoom.serializer(), LtJoinRoom(code.trim().uppercase(), username))))
    }

    fun leaveRoom() {
        send(LtMessage("leave_room", null))
        _room.value = null
        _userId.value = null
        _pendingJoin.value = emptyList()
        _messages.value = emptyList()
    }

    fun approveJoin(userId: String) {
        send(LtMessage("approve_join", json.encodeToJsonElement(LtApproveJoin.serializer(), LtApproveJoin(userId))))
    }

    fun rejectJoin(userId: String) {
        send(LtMessage("reject_join", json.encodeToJsonElement(LtRejectJoin.serializer(), LtRejectJoin(userId, "Rejected by host"))))
    }

    fun sendChat(text: String) {
        if (text.isBlank()) return
        send(LtMessage("chat", json.encodeToJsonElement(LtChat.serializer(), LtChat(text.trim()))))
    }

    fun requestSync() {
        send(LtMessage("request_sync", null))
    }

    private fun send(msg: LtMessage) {
        val ws = socket
        if (ws != null && _connected.value) {
            ws.send(json.encodeToString(LtMessage.serializer(), msg))
        } else {
            pendingAction = { ws?.send(json.encodeToString(LtMessage.serializer(), msg)) }
            connect()
        }
    }

    private fun handle(text: String) {
        val msg = try { json.decodeFromString(LtMessage.serializer(), text) } catch (e: Exception) { return }
        val p = msg.payload
        when (msg.type) {
            "room_created" -> {
                _busy.value = false
                p?.let { json.decodeFromJsonElement<LtRoomCreated>(it) }?.let { c ->
                    _userId.value = c.userId
                    _room.value = LtRoomState(c.roomCode, c.userId, listOf(LtUser(c.userId, username, true)))
                    scope.launch { _events.emit(LtEvent.RoomCreated(c.roomCode, c.userId)) }
                }
            }
            "join_approved" -> {
                _busy.value = false
                p?.let { json.decodeFromJsonElement<LtJoinApproved>(it) }?.let { c ->
                    _userId.value = c.userId
                    _room.value = c.state
                    scope.launch { _events.emit(LtEvent.Joined(c.roomCode, c.userId, c.state)) }
                }
            }
            "join_rejected" -> {
                _busy.value = false
                val reason = p?.let { json.decodeFromJsonElement<LtJoinRejected>(it).reason } ?: "Join rejected"
                scope.launch { _events.emit(LtEvent.Error(reason)) }
            }
            "join_request" -> {
                p?.let { json.decodeFromJsonElement<LtJoinRequest>(it) }?.let { r ->
                    _pendingJoin.value = _pendingJoin.value + r
                    scope.launch { _events.emit(LtEvent.JoinRequest(r.userId, r.username)) }
                }
            }
            "user_joined" -> {
                p?.let { json.decodeFromJsonElement<LtUserJoined>(it) }?.let { u ->
                    _room.value = _room.value?.let { it.copy(users = it.users + LtUser(u.userId, u.username)) }
                    scope.launch { _events.emit(LtEvent.UserJoined(u.userId, u.username)) }
                }
            }
            "user_left" -> {
                p?.let { json.decodeFromJsonElement<LtUserLeft>(it) }?.let { u ->
                    _room.value = _room.value?.let { it.copy(users = it.users.filter { x -> x.userId != u.userId }) }
                    scope.launch { _events.emit(LtEvent.UserLeft(u.userId, u.username)) }
                }
            }
            "chat" -> {
                p?.let { json.decodeFromJsonElement<LtChatMessage>(it) }?.let { c ->
                    _messages.value = (_messages.value + c).takeLast(200)
                    scope.launch { _events.emit(LtEvent.Chat(c)) }
                }
            }
            "sync_playback" -> {
                p?.let { json.decodeFromJsonElement<LtPlayback>(it) }?.let { pb ->
                    scope.launch { _events.emit(LtEvent.Playback(pb)) }
                }
            }
            "sync_state" -> {
                p?.let { json.decodeFromJsonElement<LtSyncState>(it) }?.let { s ->
                    scope.launch { _events.emit(LtEvent.SyncState(s)) }
                }
            }
            "error" -> {
                _busy.value = false
                val m = p?.let { json.decodeFromJsonElement<LtError>(it).message } ?: "Server error"
                scope.launch { _events.emit(LtEvent.Error(m)) }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------

@Composable
fun ListenTogetherScreen(
    language: String,
    onBack: () -> Unit,
    onPlaySong: (SongItem) -> Unit,
) {
    val client = remember { ListenTogetherClient() }
    val connected by client.connected.collectAsState()
    val roomState by client.room.collectAsState()
    val userIdState by client.userId.collectAsState()
    val pendingJoin by client.pendingJoin.collectAsState()
    val messages by client.messages.collectAsState()
    val busy by client.busy.collectAsState()

    var usernameInput by remember { mutableStateOf(DesktopSettings.load().listenTogetherUsername) }
    var roomCodeInput by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var chatInput by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(client) {
        client.events.collect { e ->
            when (e) {
                is LtEvent.Error -> error = e.message
                is LtEvent.RoomCreated, is LtEvent.Joined -> error = null
                else -> {}
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        BackButton(language, onBack)
        Text(Localization.get(language, "listen_together"), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))

        if (roomState == null) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        Localization.get(language, "listen_together_description"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = { usernameInput = it },
                        label = { Text(Localization.get(language, "username")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = roomCodeInput,
                        onValueChange = { if (it.length <= 8) roomCodeInput = it.uppercase() },
                        label = { Text(Localization.get(language, "room_code")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            enabled = usernameInput.isNotBlank() && !busy,
                            onClick = {
                                DesktopSettings.update { it.copy(listenTogetherUsername = usernameInput.trim()) }
                                client.createRoom(usernameInput)
                            },
                        ) { if (busy) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp) else Text(Localization.get(language, "create")) }
                        OutlinedButton(
                            enabled = usernameInput.isNotBlank() && roomCodeInput.isNotBlank() && !busy,
                            onClick = {
                                DesktopSettings.update { it.copy(listenTogetherUsername = usernameInput.trim()) }
                                client.joinRoom(roomCodeInput, usernameInput)
                            },
                        ) { Text(Localization.get(language, "connect")) }
                    }
                }
            }
        } else {
            val r = roomState!!
            val isHost = r.hostId == userIdState
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(Localization.get(language, "room_code"), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(
                        r.roomCode,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        if (isHost) Localization.get(language, "connected_users") else Localization.get(language, "connected_users"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                Localization.get(language, "connected_users") + " (${r.users.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                items(r.users, key = { it.userId }) { user ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            user.username,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        if (user.isHost) {
                            Text("👑", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                pendingJoin.forEach { req ->
                    item(key = "req-${req.userId}") {
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("${req.username} " + Localization.get(language, "connect"), modifier = Modifier.weight(1f))
                            TextButton(onClick = { client.approveJoin(req.userId) }) { Text("✓") }
                            TextButton(onClick = { client.rejectJoin(req.userId) }) { Text("✕") }
                        }
                    }
                }

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
                        Text(m.message, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = chatInput,
                    onValueChange = { chatInput = it },
                    placeholder = { Text(Localization.get(language, "comments")) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    client.sendChat(chatInput)
                    chatInput = ""
                }) { Text("➤") }
            }

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { client.requestSync() }) { Text(Localization.get(language, "connect")) }
                Button(onClick = { client.leaveRoom() }) { Text(Localization.get(language, "leave_room")) }
            }
        }
    }
}
