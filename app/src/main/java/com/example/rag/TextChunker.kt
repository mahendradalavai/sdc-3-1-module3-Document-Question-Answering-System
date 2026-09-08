package com.example.rag

object TextChunker {
    private val STOP_WORDS = setOf(
        "a", "about", "above", "after", "again", "against", "all", "am", "an", "and",
        "any", "are", "aren't", "as", "at", "be", "because", "been", "before", "being",
        "below", "between", "both", "but", "by", "can", "cannot", "could", "couldn't",
        "did", "didn't", "do", "does", "doesn't", "doing", "don't", "down", "during",
        "each", "few", "for", "from", "further", "had", "hadn't", "has", "hasn't",
        "have", "haven't", "having", "he", "he'd", "he'll", "he's", "her", "here",
        "here's", "hers", "herself", "him", "himself", "his", "how", "how's", "i",
        "i'd", "i'll", "i'm", "i've", "if", "in", "into", "is", "isn't", "it",
        "it's", "its", "itself", "let's", "me", "more", "most", "mustn't", "my",
        "myself", "no", "nor", "not", "of", "off", "on", "once", "only", "or",
        "other", "ought", "our", "ours", "ourselves", "out", "over", "own", "same",
        "shan't", "she", "she'd", "she'll", "she's", "should", "shouldn't", "so",
        "some", "such", "than", "that", "that's", "the", "their", "theirs", "them",
        "themselves", "then", "there", "there's", "these", "they", "they'd", "they'll",
        "they're", "they've", "this", "those", "through", "to", "too", "under", "until",
        "up", "very", "was", "wasn't", "we", "we'd", "we'll", "we're", "we've",
        "were", "weren't", "what", "what's", "when", "when's", "where", "where's",
        "which", "while", "who", "who's", "whom", "why", "why's", "with", "won't",
        "would", "wouldn't", "you", "you'd", "you'll", "you're", "you've", "your",
        "yours", "yourself", "yourselves"
    )

    data class ChunkResult(
        val index: Int,
        val text: String,
        val tokenCount: Int,
        val keywords: String
    )

    /**
     * Splits input text into sliding chunks with paragraph and sentence boundary awareness.
     */
    fun chunkText(
        text: String,
        targetChunkSize: Int = 400,
        overlapSize: Int = 80
    ): List<ChunkResult> {
        val cleanText = text.trim()
        if (cleanText.isEmpty()) return emptyList()

        val paragraphs = cleanText.split(Regex("\n{2,}"))
        val chunks = mutableListOf<String>()

        var currentBuffer = StringBuilder()

        for (paragraph in paragraphs) {
            val p = paragraph.trim()
            if (p.isEmpty()) continue

            if (currentBuffer.length + p.length <= targetChunkSize) {
                if (currentBuffer.isNotEmpty()) currentBuffer.append("\n\n")
                currentBuffer.append(p)
            } else {
                // If paragraph itself is longer than targetChunkSize, split by sentence
                val sentences = p.split(Regex("(?<=[.!?])\\s+"))
                for (sentence in sentences) {
                    val s = sentence.trim()
                    if (s.isEmpty()) continue

                    if (currentBuffer.length + s.length > targetChunkSize && currentBuffer.isNotEmpty()) {
                        chunks.add(currentBuffer.toString())
                        // Retain overlap from end of current buffer
                        val prevText = currentBuffer.toString()
                        val overlapText = if (prevText.length > overlapSize) {
                            prevText.substring(prevText.length - overlapSize)
                        } else {
                            ""
                        }
                        currentBuffer = StringBuilder()
                        if (overlapText.isNotBlank()) {
                            currentBuffer.append(overlapText.trim()).append(" ")
                        }
                    }
                    currentBuffer.append(s).append(" ")
                }
            }
        }

        if (currentBuffer.isNotBlank()) {
            chunks.add(currentBuffer.toString().trim())
        }

        return chunks.mapIndexed { index, chunkStr ->
            val words = extractKeywords(chunkStr)
            val approxTokens = (chunkStr.length / 4).coerceAtLeast(words.size)
            ChunkResult(
                index = index,
                text = chunkStr.trim(),
                tokenCount = approxTokens,
                keywords = words.take(20).joinToString(",")
            )
        }
    }

    /**
     * Extract normalized unique keywords without stop words.
     */
    fun extractKeywords(text: String): List<String> {
        return text.lowercase()
            .split(Regex("[^a-z0-9_\\-]+"))
            .filter { it.length > 2 && it !in STOP_WORDS }
            .distinct()
    }
}
