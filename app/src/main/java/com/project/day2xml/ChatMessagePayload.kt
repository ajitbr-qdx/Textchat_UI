package com.project.day2xml

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ChatMessagePayload(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "sender") val sender: String,
    @field:Json(name = "text") val text: String,
    @field:Json(name = "timestamp") val timestamp: Long = System.currentTimeMillis()
)
