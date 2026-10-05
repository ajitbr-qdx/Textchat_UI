package com.project.day2xml

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testChatMessagePayloadSerialization() {
        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
        val adapter = moshi.adapter(ChatMessagePayload::class.java)

        val payload = ChatMessagePayload(
            id = "123",
            sender = "User1",
            text = "Hello WebSocket!",
            timestamp = 1700000000000L
        )

        val json = adapter.toJson(payload)
        assertNotNull(json)

        val deserialized = adapter.fromJson(json)
        assertNotNull(deserialized)
        assertEquals("123", deserialized?.id)
        assertEquals("User1", deserialized?.sender)
        assertEquals("Hello WebSocket!", deserialized?.text)
        assertEquals(1700000000000L, deserialized?.timestamp)
    }
}
