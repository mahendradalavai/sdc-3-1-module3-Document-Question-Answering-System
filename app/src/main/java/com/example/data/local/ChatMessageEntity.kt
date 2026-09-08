package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val role: String, // "user" or "assistant"
    val text: String,
    val sourcesJson: String? = null, // serialized List<CitationSource>
    val timestamp: Long = System.currentTimeMillis(),
    val responseTimeMs: Long = 0L,
    val retrievedChunkCount: Int = 0
)
