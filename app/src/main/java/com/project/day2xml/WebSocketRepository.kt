package com.project.day2xml

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

class WebSocketRepository(
    private val webSocketManager: WebSocketManager = WebSocketManager()
) {

    val connectionState: StateFlow<ConnectionState> = webSocketManager.connectionState
    val incomingMessages: Flow<ChatMessagePayload> = webSocketManager.incomingMessages
    val systemMessages: Flow<String> = webSocketManager.systemMessages

    fun connect(url: String) {
        webSocketManager.connect(url)
    }

    fun disconnect() {
        webSocketManager.disconnect()
    }

    fun sendMessage(text: String, sender: String = "Me"): ChatMessagePayload? {
        return webSocketManager.sendMessage(text, sender)
    }
}
