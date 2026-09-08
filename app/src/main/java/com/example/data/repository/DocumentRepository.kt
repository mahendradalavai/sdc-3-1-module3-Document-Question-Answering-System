package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ChunkEntity
import com.example.data.local.DocumentEntity
import com.example.data.local.SampleDocuments
import com.example.rag.GeminiRagService
import com.example.rag.RagResponse
import com.example.rag.RagRetriever
import com.example.rag.RetrievalResult
import com.example.rag.TextChunker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class DocumentRepository(private val database: AppDatabase) {

    private val documentDao = database.documentDao()
    private val chatDao = database.chatDao()

    val allDocuments: Flow<List<DocumentEntity>> = documentDao.getAllDocuments()
    val allChunks: Flow<List<ChunkEntity>> = documentDao.getAllChunksFlow()
    val chatMessages: Flow<List<ChatMessageEntity>> = chatDao.getAllMessages()

    /**
     * Initializes knowledge base with sample docs if database is fresh.
     */
    suspend fun seedInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        val count = documentDao.getDocumentCount()
        if (count == 0) {
            for (sample in SampleDocuments.list) {
                addDocument(
                    title = sample.title,
                    category = sample.category,
                    content = sample.content
                )
            }
        }
    }

    /**
     * Ingests a new document: splits into chunks, indexes keywords,
     * attempts embedding generation, and persists in Room.
     */
    suspend fun addDocument(
        title: String,
        category: String,
        content: String
    ): Long = withContext(Dispatchers.IO) {
        val cleanContent = content.trim()
        val chunkResults = TextChunker.chunkText(cleanContent)

        val docEntity = DocumentEntity(
            title = title.trim(),
            category = category.trim().ifEmpty { "General" },
            content = cleanContent,
            charCount = cleanContent.length,
            chunkCount = chunkResults.size
        )

        val docId = documentDao.insertDocument(docEntity)

        val chunkEntities = chunkResults.map { chunkRes ->
            // Try fetching embedding if API key is active
            var embeddingStr: String? = null
            if (GeminiRagService.isApiKeyConfigured()) {
                val emb = GeminiRagService.getEmbedding(chunkRes.text)
                if (emb != null) {
                    embeddingStr = emb.joinToString(",")
                }
            }

            ChunkEntity(
                documentId = docId,
                documentTitle = docEntity.title,
                chunkIndex = chunkRes.index,
                content = chunkRes.text,
                tokenCount = chunkRes.tokenCount,
                embeddingValues = embeddingStr,
                keywords = chunkRes.keywords
            )
        }

        documentDao.insertChunks(chunkEntities)
        docId
    }

    /**
     * Deletes a document and its cascade chunks.
     */
    suspend fun deleteDocument(id: Long) = withContext(Dispatchers.IO) {
        documentDao.deleteDocument(id)
    }

    /**
     * Resets knowledge base back to default sample documents.
     */
    suspend fun resetToSampleDocuments() = withContext(Dispatchers.IO) {
        documentDao.deleteAllDocuments()
        for (sample in SampleDocuments.list) {
            addDocument(
                title = sample.title,
                category = sample.category,
                content = sample.content
            )
        }
    }

    /**
     * Clears all chat history.
     */
    suspend fun clearChat() = withContext(Dispatchers.IO) {
        chatDao.clearChat()
    }

    /**
     * Executes RAG workflow:
     * 1. Fetches all knowledge base chunks from Room.
     * 2. Runs vector embedding / BM25 hybrid retrieval to locate Top-K chunks.
     * 3. Augments prompt and queries Gemini 3.5 Flash (with fallback synthesis).
     * 4. Persists conversation turns in ChatDao.
     */
    suspend fun answerQuestion(
        question: String
    ): Pair<RagResponse, RetrievalResult> = withContext(Dispatchers.IO) {
        // Record user query in chat
        chatDao.insertMessage(
            ChatMessageEntity(
                role = "user",
                text = question.trim()
            )
        )

        // 1. Retrieve all chunks
        val allLocalChunks = documentDao.getAllChunks()

        // 2. Query embedding if API key available
        val queryEmbedding = if (GeminiRagService.isApiKeyConfigured()) {
            GeminiRagService.getEmbedding(question)
        } else {
            null
        }

        // 3. Retrieval
        val retrievalResult = RagRetriever.retrieveTopChunks(
            query = question,
            chunks = allLocalChunks,
            queryEmbedding = queryEmbedding,
            topK = 4
        )

        // 4. Generation
        val ragResponse = GeminiRagService.generateRagAnswer(
            question = question,
            retrievedChunks = retrievalResult.topChunks
        )

        // Serialize citation sources to JSON
        val sourcesJsonArray = JSONArray()
        for (src in ragResponse.sources) {
            val obj = JSONObject().apply {
                put("docId", src.documentId)
                put("docTitle", src.documentTitle)
                put("chunkIndex", src.chunkIndex)
                put("snippet", src.textSnippet)
                put("score", src.relevanceScore.toDouble())
            }
            sourcesJsonArray.put(obj)
        }

        // 5. Persist assistant response
        chatDao.insertMessage(
            ChatMessageEntity(
                role = "assistant",
                text = ragResponse.answer,
                sourcesJson = sourcesJsonArray.toString(),
                responseTimeMs = ragResponse.responseTimeMs,
                retrievedChunkCount = ragResponse.sources.size
            )
        )

        Pair(ragResponse, retrievalResult)
    }
}
