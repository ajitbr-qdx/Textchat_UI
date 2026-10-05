package com.project.day2xml

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.UUID
import java.util.concurrent.TimeUnit

class WebSocketManager(
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<ChatMessagePayload>(extraBufferCapacity = 64)
    val incomingMessages: SharedFlow<ChatMessagePayload> = _incomingMessages.asSharedFlow()

    private val _systemMessages = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val systemMessages: SharedFlow<String> = _systemMessages.asSharedFlow()

    private val client: OkHttpClient = OkHttpClient.Builder()
        .pingInterval(15, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val jsonAdapter = moshi.adapter(ChatMessagePayload::class.java)

    private var webSocket: WebSocket? = null
    private var serverUrl: String = "wss://echo.websocket.org"

    private var reconnectAttempts = 0
    private var isManualDisconnect = false
    private var reconnectJob: Job? = null

    private val initialDelayMs = 1000L
    private val maxDelayMs = 32000L

    fun connect(url: String = serverUrl) {
        this.serverUrl = url
        this.isManualDisconnect = false
        this.reconnectAttempts = 0
        reconnectJob?.cancel()
        connectInternal()
    }

    private fun connectInternal() {
        if (_connectionState.value == ConnectionState.CONNECTED) return

        _connectionState.value = ConnectionState.CONNECTING
        postSystemMessage("Connecting to $serverUrl...")

        val request = Request.Builder()
            .url(serverUrl)
            .build()

        webSocket = client.newWebSocket(request, createWebSocketListener())
    }

    fun disconnect() {
        isManualDisconnect = true
        reconnectJob?.cancel()
        webSocket?.close(1000, "Client disconnected")
        webSocket = null
        _connectionState.value = ConnectionState.DISCONNECTED
        postSystemMessage("Disconnected by user")
    }

    fun sendMessage(text: String, sender: String = "Me"): ChatMessagePayload? {
        if (_connectionState.value != ConnectionState.CONNECTED) {
            postSystemMessage("Cannot send message: Not connected")
            return null
        }

        val payload = ChatMessagePayload(
            id = UUID.randomUUID().toString(),
            sender = sender,
            text = text,
            timestamp = System.currentTimeMillis()
        )

        val jsonString = try {
            jsonAdapter.toJson(payload)
        } catch (_: Exception) {
            text // fallback to raw text
        }

        val sent = webSocket?.send(jsonString) ?: false
        return if (sent) {
            payload
        } else {
            postSystemMessage("Failed to send message over WebSocket")
            null
        }
    }

    private fun createWebSocketListener(): WebSocketListener {
        return object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: Response) {
                reconnectAttempts = 0
                _connectionState.value = ConnectionState.CONNECTED
                postSystemMessage("WebSocket connected successfully!")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleReceivedText(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                handleReceivedText(bytes.utf8())
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                postSystemMessage("WebSocket closing: $reason (code: $code)")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _connectionState.value = ConnectionState.DISCONNECTED
                postSystemMessage("WebSocket closed: $reason")
                if (!isManualDisconnect) {
                    scheduleReconnect()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _connectionState.value = ConnectionState.DISCONNECTED
                postSystemMessage("Connection error: ${t.localizedMessage ?: "Network failure"}")
                if (!isManualDisconnect) {
                    scheduleReconnect()
                }
            }
        }
    }

    private fun handleReceivedText(text: String) {
        val payload = try {
            jsonAdapter.fromJson(text) ?: ChatMessagePayload(
                id = UUID.randomUUID().toString(),
                sender = "Echo Server",
                text = text,
                timestamp = System.currentTimeMillis()
            )
        } catch (_: Exception) {
            ChatMessagePayload(
                id = UUID.randomUUID().toString(),
                sender = "Echo Server",
                text = text,
                timestamp = System.currentTimeMillis()
            )
        }

        externalScope.launch {
            _incomingMessages.emit(payload)
        }
    }

    private fun scheduleReconnect() {
        if (isManualDisconnect) return

        val shift = minOf(reconnectAttempts, 10)
        val delayMs = minOf(maxDelayMs, initialDelayMs * (1L shl shift))
        reconnectAttempts++

        _connectionState.value = ConnectionState.RECONNECTING
        postSystemMessage("Reconnecting in ${delayMs / 1000}s (Attempt #$reconnectAttempts)...")

        reconnectJob?.cancel()
        reconnectJob = externalScope.launch {
            delay(delayMs)
            postSystemMessage("Attempting to reconnect...")
            connectInternal()
        }
    }

    private fun postSystemMessage(message: String) {
        externalScope.launch {
            _systemMessages.emit(message)
        }
    }
}
