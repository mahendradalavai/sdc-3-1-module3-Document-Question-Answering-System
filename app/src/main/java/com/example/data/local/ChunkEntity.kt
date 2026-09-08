package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "document_chunks",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("documentId"),
        Index("chunkIndex")
    ]
)
data class ChunkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val documentId: Long,
    val documentTitle: String,
    val chunkIndex: Int,
    val content: String,
    val tokenCount: Int,
    val embeddingValues: String? = null, // comma-separated floats
    val keywords: String = "" // extracted key tokens
)
