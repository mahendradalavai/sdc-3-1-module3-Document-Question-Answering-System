package com.example

import com.example.rag.RagRetriever
import com.example.rag.TextChunker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun textChunker_emptyText_returnsEmptyList() {
    val chunks = TextChunker.chunkText("")
    assertTrue(chunks.isEmpty())
  }

  @Test
  fun textChunker_chunksLongTextWithKeywords() {
    val text = "Artificial intelligence and machine learning transform information retrieval. " +
        "Vector search uses high-dimensional embeddings to match semantic intent across document archives."
    val chunks = TextChunker.chunkText(text, targetChunkSize = 80, overlapSize = 20)
    assertTrue("Should produce at least one chunk", chunks.isNotEmpty())
    assertTrue("Keywords should be extracted", chunks.first().keywords.isNotEmpty())
  }

  @Test
  fun textChunker_extractKeywords_removesStopWords() {
    val keywords = TextChunker.extractKeywords("The quick brown fox jumps over the lazy dog and this that")
    assertTrue(keywords.contains("quick"))
    assertTrue(keywords.contains("brown"))
    assertTrue("Should exclude stop word 'the'", !keywords.contains("the"))
    assertTrue("Should exclude stop word 'and'", !keywords.contains("and"))
  }

  @Test
  fun ragRetriever_parseEmbedding_parsesCorrectly() {
    val csv = "0.1, 0.25, -0.5, 1.0"
    val embedding = RagRetriever.parseEmbedding(csv)
    assertNotNull(embedding)
    assertEquals(4, embedding?.size)
    assertEquals(0.25f, embedding?.get(1) ?: 0f, 0.001f)
  }
}

