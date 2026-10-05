package com.project.day2xml

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: WebSocketRepository = WebSocketRepository()
) : ViewModel() {

    val connectionState: StateFlow<ConnectionState> = repository.connectionState

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    init {
        observeIncomingMessages()
        observeSystemMessages()
    }

    fun connect(url: String) {
        repository.connect(url)
    }

    fun disconnect() {
        repository.disconnect()
    }

    fun sendMessage(text: String) {
        val payload = repository.sendMessage(text, sender = "You")
        if (payload != null) {
            val sentMessage = Message(
                id = payload.id,
                text = payload.text,
                type = MessageType.TEXT_SENT,
                sender = "You",
                timestamp = payload.timestamp
            )
            appendMessage(sentMessage)
        }
    }

    private fun observeIncomingMessages() {
        viewModelScope.launch {
            repository.incomingMessages.collect { payload ->
                val message = Message(
                    id = payload.id,
                    text = payload.text,
                    type = MessageType.TEXT_RECEIVED,
                    sender = payload.sender.ifEmpty { "Echo Server" },
                    timestamp = payload.timestamp
                )
                appendMessage(message)
            }
        }
    }

    private fun observeSystemMessages() {
        viewModelScope.launch {
            repository.systemMessages.collect { text ->
                val message = Message(
                    id = System.currentTimeMillis().toString(),
                    text = text,
                    type = MessageType.SYSTEM,
                    sender = "System"
                )
                appendMessage(message)
            }
        }
    }

    private fun appendMessage(message: Message) {
        _messages.value = _messages.value + message
    }

    override fun onCleared() {
        super.onCleared()
        repository.disconnect()
    }
}
