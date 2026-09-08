package com.example.ui

import com.example.data.local.ChatMessageEntity
import com.example.data.local.ChunkEntity
import com.example.data.local.DocumentEntity
import com.example.rag.CitationSource
import com.example.rag.RetrievalResult

enum class NavTab {
    ASSISTANT,
    KNOWLEDGE_BASE,
    RAG_INSPECTOR
}

data class RagUiState(
    val activeTab: NavTab = NavTab.ASSISTANT,
    val queryInput: String = "",
    val isLoading: Boolean = false,
    val documents: List<DocumentEntity> = emptyList(),
    val chunks: List<ChunkEntity> = emptyList(),
    val chatMessages: List<ChatMessageEntity> = emptyList(),
    val selectedCitation: CitationSource? = null,
    val inspectingDocument: DocumentEntity? = null,
    val inspectingDocumentChunks: List<ChunkEntity> = emptyList(),
    val lastRetrievalResult: RetrievalResult? = null,
    val lastPromptSent: String? = null,
    val isApiKeyActive: Boolean = false,
    val showAddDialog: Boolean = false,
    val addDocTitle: String = "",
    val addDocCategory: String = "Technical",
    val addDocContent: String = "",
    val feedbackMessage: String? = null
)
