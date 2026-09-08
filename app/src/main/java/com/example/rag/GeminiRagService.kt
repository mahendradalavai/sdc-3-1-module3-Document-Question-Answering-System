package com.example.rag

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiRagService {

    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta"
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val EMBEDDING_MODEL = "gemini-embedding-2-preview"

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Checks whether a configured Gemini API key is available.
     */
    fun isApiKeyConfigured(): Boolean {
        val key = BuildConfig.GEMINI_API_KEY
        return !key.isNullOrBlank() && key != "MY_GEMINI_API_KEY"
    }

    /**
     * Obtains vector embeddings for text using gemini-embedding-2-preview.
     * Returns null if API key is not configured or network call fails.
     */
    suspend fun getEmbedding(text: String): List<Float>? = withContext(Dispatchers.IO) {
        if (!isApiKeyConfigured()) return@withContext null

        val apiKey = BuildConfig.GEMINI_API_KEY
        val url = "$BASE_URL/models/$EMBEDDING_MODEL:embedContent?key=$apiKey"

        val jsonBody = JSONObject().apply {
            put("model", "models/$EMBEDDING_MODEL")
            put("content", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", text.take(1500)) })
                })
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val bodyStr = response.body?.string() ?: return@withContext null
                val json = JSONObject(bodyStr)
                val embeddingObj = json.optJSONObject("embedding") ?: return@withContext null
                val valuesArr = embeddingObj.optJSONArray("values") ?: return@withContext null

                val list = mutableListOf<Float>()
                for (i in 0 until valuesArr.length()) {
                    list.add(valuesArr.getDouble(i).toFloat())
                }
                list
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Generates an answer using RAG: Augments user question with retrieved chunks
     * and queries Gemini 3.5 Flash. Falls back to smart local synthesis if offline/unkeyed.
     */
    suspend fun generateRagAnswer(
        question: String,
        retrievedChunks: List<ScoredChunk>
    ): RagResponse = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        // Prepare citation sources
        val citations = retrievedChunks.mapIndexed { index, chunk ->
            val relevancePercent = (chunk.score * 100f).coerceIn(10f, 99f)
            CitationSource(
                documentId = chunk.documentId,
                documentTitle = chunk.documentTitle,
                chunkIndex = chunk.chunkIndex,
                textSnippet = chunk.content,
                relevanceScore = relevancePercent,
                matchedKeywords = chunk.matchedTerms
            )
        }

        // Build Augmented Prompt
        val promptBuilder = StringBuilder()
        promptBuilder.append("You are DocuQuery, an expert RAG (Retrieval-Augmented Generation) assistant.\n")
        promptBuilder.append("Your mission is to provide an accurate, clear answer to the user's question based strictly on the provided retrieved document context below.\n\n")
        promptBuilder.append("CRITICAL INSTRUCTIONS:\n")
        promptBuilder.append("1. Use ONLY the information in the CONTEXT below. Do not make up facts or extrapolate beyond what is documented.\n")
        promptBuilder.append("2. Cite your sources directly in the answer using [Source 1], [Source 2], etc. matching the context items.\n")
        promptBuilder.append("3. If the context does not contain enough info to answer the question, honestly state: 'The provided documents do not contain enough information to answer this question.'\n")
        promptBuilder.append("4. Format with clean bullet points and bold key terms where appropriate.\n\n")

        promptBuilder.append("=== RETRIEVED CONTEXT ===\n")
        if (retrievedChunks.isEmpty()) {
            promptBuilder.append("[No relevant document chunks found in the knowledge base]\n")
        } else {
            retrievedChunks.forEachIndexed { i, chunk ->
                promptBuilder.append("--- [Source ${i + 1}: \"${chunk.documentTitle}\" (Chunk #${chunk.chunkIndex + 1})] ---\n")
                promptBuilder.append("${chunk.content}\n\n")
            }
        }
        promptBuilder.append("=== END OF CONTEXT ===\n\n")
        promptBuilder.append("USER QUESTION:\n$question\n")

        val augmentedPrompt = promptBuilder.toString()

        // If API key is available, call Gemini 3.5 Flash
        if (isApiKeyConfigured()) {
            val apiKey = BuildConfig.GEMINI_API_KEY
            val url = "$BASE_URL/models/$MODEL_NAME:generateContent?key=$apiKey"

            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", augmentedPrompt) })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.3) // Low temperature for high factual adherence in RAG
                    put("topP", 0.9)
                })
            }

            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            try {
                httpClient.newCall(request).execute().use { response ->
                    val elapsed = System.currentTimeMillis() - startTime
                    if (response.isSuccessful) {
                        val bodyStr = response.body?.string()
                        if (!bodyStr.isNullOrEmpty()) {
                            val json = JSONObject(bodyStr)
                            val candidates = json.optJSONArray("candidates")
                            val firstCandidate = candidates?.optJSONObject(0)
                            val contentObj = firstCandidate?.optJSONObject("content")
                            val parts = contentObj?.optJSONArray("parts")
                            val text = parts?.optJSONObject(0)?.optString("text")

                            if (!text.isNullOrBlank()) {
                                return@withContext RagResponse(
                                    answer = text.trim(),
                                    sources = citations,
                                    promptSent = augmentedPrompt,
                                    responseTimeMs = elapsed,
                                    isFallbackSynthesis = false
                                )
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                // Network or parsing error, proceed to fallback
            }
        }

        // Local Intelligent Extractive Fallback
        val elapsed = System.currentTimeMillis() - startTime
        val fallbackAnswer = buildLocalExtractiveAnswer(question, retrievedChunks)

        RagResponse(
            answer = fallbackAnswer,
            sources = citations,
            promptSent = augmentedPrompt,
            responseTimeMs = elapsed,
            isFallbackSynthesis = true
        )
    }

    /**
     * Local deterministic extractive synthesis when Gemini API key is not present.
     * Highlights exact relevant excerpts and attributes to source chunks.
     */
    private fun buildLocalExtractiveAnswer(
        question: String,
        chunks: List<ScoredChunk>
    ): String {
        if (chunks.isEmpty()) {
            return "No relevant documents found in the local knowledge base to answer '$question'. Please add or import documents to get started."
        }

        val sb = StringBuilder()
        sb.append("Based on the retrieved documents in the knowledge base:\n\n")

        val queryKeywords = TextChunker.extractKeywords(question)

        chunks.take(3).forEachIndexed { i, chunk ->
            // Extract the most relevant sentence(s) in this chunk
            val sentences = chunk.content.split(Regex("(?<=[.!?])\\s+"))
            val scoredSentences = sentences.map { sentence ->
                val count = queryKeywords.count { kw -> sentence.contains(kw, ignoreCase = true) }
                sentence to count
            }.sortedByDescending { it.second }

            val topSentence = scoredSentences.firstOrNull { it.second > 0 }?.first
                ?: sentences.firstOrNull()
                ?: chunk.content

            sb.append("• **${chunk.documentTitle}** [Source ${i + 1}]:\n")
            sb.append("  \"${topSentence.trim()}\"\n\n")
        }

        if (!isApiKeyConfigured()) {
            sb.append("\n💡 *Tip: Add your `GEMINI_API_KEY` in the AI Studio Secrets panel to enable full generative synthesis with Gemini 3.5 Flash.*")
        }

        return sb.toString()
    }
}
