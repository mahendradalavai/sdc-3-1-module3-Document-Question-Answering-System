package com.example.rag

data class CitationSource(
    val documentId: Long,
    val documentTitle: String,
    val chunkIndex: Int,
    val textSnippet: String,
    val relevanceScore: Float,
    val matchedKeywords: List<String> = emptyList()
)

data class RetrievalResult(
    val query: String,
    val topChunks: List<ScoredChunk>,
    val executionTimeMs: Long,
    val totalChunksSearched: Int,
    val methodUsed: String // "Vector Embedding + BM25 Hybrid" or "BM25 Keyword Scoring"
)

data class ScoredChunk(
    val chunkId: Long,
    val documentId: Long,
    val documentTitle: String,
    val chunkIndex: Int,
    val content: String,
    val score: Float,
    val vectorScore: Float = 0f,
    val bm25Score: Float = 0f,
    val matchedTerms: List<String> = emptyList()
)

data class RagResponse(
    val answer: String,
    val sources: List<CitationSource>,
    val promptSent: String,
    val responseTimeMs: Long,
    val isFallbackSynthesis: Boolean = false
)
