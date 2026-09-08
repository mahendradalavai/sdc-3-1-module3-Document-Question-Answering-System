package com.example.rag

import com.example.data.local.ChunkEntity
import kotlin.math.ln
import kotlin.math.sqrt

object RagRetriever {

    /**
     * Performs hybrid retrieval over chunks using Lexical (BM25/TF-IDF)
     * and Vector Cosine Similarity (when embeddings are available).
     */
    fun retrieveTopChunks(
        query: String,
        chunks: List<ChunkEntity>,
        queryEmbedding: List<Float>? = null,
        topK: Int = 4
    ): RetrievalResult {
        val startTime = System.currentTimeMillis()
        if (chunks.isEmpty() || query.isBlank()) {
            return RetrievalResult(
                query = query,
                topChunks = emptyList(),
                executionTimeMs = 0L,
                totalChunksSearched = chunks.size,
                methodUsed = "Empty Search"
            )
        }

        val queryKeywords = TextChunker.extractKeywords(query)
        val hasVectorQuery = queryEmbedding != null && queryEmbedding.isNotEmpty()

        // Precompute document frequency for BM25
        val totalDocs = chunks.size.toDouble()
        val docFrequency = mutableMapOf<String, Int>()
        for (kw in queryKeywords) {
            val count = chunks.count { it.keywords.contains(kw) || it.content.contains(kw, ignoreCase = true) }
            docFrequency[kw] = count.coerceAtLeast(1)
        }

        val scoredChunks = chunks.map { chunk ->
            // 1. Calculate Lexical (BM25-style) score
            val chunkTextLower = chunk.content.lowercase()
            val chunkTitleLower = chunk.documentTitle.lowercase()
            val matchedTerms = mutableListOf<String>()

            var bm25Score = 0.0
            for (term in queryKeywords) {
                val tfInContent = countOccurrences(chunkTextLower, term)
                val tfInTitle = countOccurrences(chunkTitleLower, term)

                if (tfInContent > 0 || tfInTitle > 0) {
                    matchedTerms.add(term)
                    val df = docFrequency[term] ?: 1
                    val idf = ln((totalDocs - df + 0.5) / (df + 0.5) + 1.0).coerceAtLeast(0.1)

                    // BM25 term weighting
                    val k1 = 1.2
                    val b = 0.75
                    val avgDocLen = 80.0
                    val docLen = chunk.tokenCount.coerceAtLeast(10).toDouble()
                    val tfNorm = (tfInContent * (k1 + 1)) / (tfInContent + k1 * (1 - b + b * (docLen / avgDocLen)))

                    // Boost title matches significantly
                    val titleBonus = if (tfInTitle > 0) 2.5 else 0.0

                    bm25Score += (idf * tfNorm) + titleBonus
                }
            }

            // Phrase match bonus
            val cleanQuery = query.trim().lowercase()
            if (cleanQuery.length > 5 && chunkTextLower.contains(cleanQuery)) {
                bm25Score += 4.0
            }

            // 2. Calculate Vector Cosine Similarity if embedding available
            var vectorScore = 0f
            if (hasVectorQuery && !chunk.embeddingValues.isNullOrEmpty()) {
                val chunkEmbedding = parseEmbedding(chunk.embeddingValues)
                if (chunkEmbedding != null && chunkEmbedding.size == queryEmbedding.size) {
                    vectorScore = cosineSimilarity(queryEmbedding, chunkEmbedding).toFloat()
                }
            }

            // Normalize BM25 score to 0..1 scale roughly
            val normBm25 = (bm25Score / (bm25Score + 2.0)).toFloat()

            // Hybrid combination: weight vector 60%, BM25 40% when vector is available
            val combinedScore = if (hasVectorQuery && vectorScore > 0f) {
                (vectorScore * 0.65f) + (normBm25 * 0.35f)
            } else {
                normBm25
            }

            ScoredChunk(
                chunkId = chunk.id,
                documentId = chunk.documentId,
                documentTitle = chunk.documentTitle,
                chunkIndex = chunk.chunkIndex,
                content = chunk.content,
                score = combinedScore,
                vectorScore = vectorScore,
                bm25Score = normBm25,
                matchedTerms = matchedTerms
            )
        }

        // Filter out zero-relevance chunks unless everything is zero, then take top K
        val filtered = scoredChunks
            .filter { it.score > 0.05f }
            .sortedByDescending { it.score }
            .take(topK)

        val finalChunks = if (filtered.isEmpty()) {
            scoredChunks.sortedByDescending { it.score }.take(topK)
        } else {
            filtered
        }

        val elapsed = System.currentTimeMillis() - startTime
        val method = if (hasVectorQuery) "Vector Embedding (Gemini) + BM25 Hybrid" else "BM25 Lexical Keyword Retrieval"

        return RetrievalResult(
            query = query,
            topChunks = finalChunks,
            executionTimeMs = elapsed,
            totalChunksSearched = chunks.size,
            methodUsed = method
        )
    }

    private fun countOccurrences(text: String, word: String): Int {
        var count = 0
        var index = 0
        while (true) {
            val found = text.indexOf(word, index)
            if (found == -1) break
            count++
            index = found + word.length
        }
        return count
    }

    private fun cosineSimilarity(vecA: List<Float>, vecB: List<Float>): Double {
        var dot = 0.0
        var normA = 0.0
        var normB = 0.0
        val n = minOf(vecA.size, vecB.size)
        for (i in 0 until n) {
            val a = vecA[i].toDouble()
            val b = vecB[i].toDouble()
            dot += a * b
            normA += a * a
            normB += b * b
        }
        val denominator = sqrt(normA) * sqrt(normB)
        return if (denominator > 0.0) dot / denominator else 0.0
    }

    fun parseEmbedding(csv: String): List<Float>? {
        return try {
            csv.split(",")
                .mapNotNull { it.trim().toFloatOrNull() }
                .takeIf { it.isNotEmpty() }
        } catch (_: Exception) {
            null
        }
    }
}
