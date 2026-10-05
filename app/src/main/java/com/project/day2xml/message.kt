package com.project.day2xml

data class Message(
    val id: String,
    val text: String,
    val type: MessageType,
    val sender: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
