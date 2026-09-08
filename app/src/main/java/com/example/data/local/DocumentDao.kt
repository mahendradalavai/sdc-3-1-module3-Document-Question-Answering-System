package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents ORDER BY createdAt DESC")
    fun getAllDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getDocumentById(id: Long): DocumentEntity?

    @Query("SELECT COUNT(*) FROM documents")
    suspend fun getDocumentCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity): Long

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteDocument(id: Long)

    @Query("DELETE FROM documents")
    suspend fun deleteAllDocuments()

    @Query("SELECT * FROM document_chunks ORDER BY documentId, chunkIndex ASC")
    fun getAllChunksFlow(): Flow<List<ChunkEntity>>

    @Query("SELECT * FROM document_chunks ORDER BY documentId, chunkIndex ASC")
    suspend fun getAllChunks(): List<ChunkEntity>

    @Query("SELECT * FROM document_chunks WHERE documentId = :documentId ORDER BY chunkIndex ASC")
    suspend fun getChunksForDocument(documentId: Long): List<ChunkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunks(chunks: List<ChunkEntity>)

    @Query("DELETE FROM document_chunks WHERE documentId = :documentId")
    suspend fun deleteChunksForDocument(documentId: Long)

    @Query("UPDATE document_chunks SET embeddingValues = :embedding WHERE id = :chunkId")
    suspend fun updateChunkEmbedding(chunkId: Long, embedding: String)
}
