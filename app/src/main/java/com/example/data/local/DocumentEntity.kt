package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val category: String,
    val content: String,
    val charCount: Int,
    val chunkCount: Int,
    val createdAt: Long = System.currentTimeMillis()
)
