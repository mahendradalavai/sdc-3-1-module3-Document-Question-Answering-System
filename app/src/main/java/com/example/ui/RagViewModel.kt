package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.DocumentEntity
import com.example.data.repository.DocumentRepository
import com.example.rag.CitationSource
import com.example.rag.GeminiRagService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray

class RagViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DocumentRepository
    private val _uiState = MutableStateFlow(RagUiState())
    val uiState: StateFlow<RagUiState> = _uiState.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = DocumentRepository(db)

        _uiState.update {
            it.copy(isApiKeyActive = GeminiRagService.isApiKeyConfigured())
        }

        // Seed initial documents if empty
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }

        // Observe documents
        viewModelScope.launch {
            repository.allDocuments.collect { docs ->
                _uiState.update { it.copy(documents = docs) }
            }
        }

        // Observe chunks
        viewModelScope.launch {
            repository.allChunks.collect { chunks ->
                _uiState.update { it.copy(chunks = chunks) }
            }
        }

        // Observe chat messages
        viewModelScope.launch {
            repository.chatMessages.collect { messages ->
                _uiState.update { it.copy(chatMessages = messages) }
            }
        }
    }

    fun onTabSelected(tab: NavTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun onQueryChange(newQuery: String) {
        _uiState.update { it.copy(queryInput = newQuery) }
    }

    fun askQuestion(overrideQuery: String? = null) {
        val query = (overrideQuery ?: _uiState.value.queryInput).trim()
        if (query.isBlank() || _uiState.value.isLoading) return

        _uiState.update {
            it.copy(
                queryInput = "",
                isLoading = true,
                activeTab = NavTab.ASSISTANT
            )
        }

        viewModelScope.launch {
            try {
                val (response, retrievalResult) = repository.answerQuestion(query)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        lastRetrievalResult = retrievalResult,
                        lastPromptSent = response.promptSent
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        feedbackMessage = "Error processing question: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    fun onSelectCitation(citation: CitationSource?) {
        _uiState.update { it.copy(selectedCitation = citation) }
    }

    fun inspectDocument(doc: DocumentEntity?) {
        if (doc == null) {
            _uiState.update {
                it.copy(
                    inspectingDocument = null,
                    inspectingDocumentChunks = emptyList()
                )
            }
        } else {
            val docChunks = _uiState.value.chunks.filter { it.documentId == doc.id }
            _uiState.update {
                it.copy(
                    inspectingDocument = doc,
                    inspectingDocumentChunks = docChunks
                )
            }
        }
    }

    fun openAddDialog() {
        _uiState.update {
            it.copy(
                showAddDialog = true,
                addDocTitle = "",
                addDocCategory = "Technical",
                addDocContent = ""
            )
        }
    }

    fun closeAddDialog() {
        _uiState.update { it.copy(showAddDialog = false) }
    }

    fun onAddDocTitleChange(title: String) {
        _uiState.update { it.copy(addDocTitle = title) }
    }

    fun onAddDocCategoryChange(category: String) {
        _uiState.update { it.copy(addDocCategory = category) }
    }

    fun onAddDocContentChange(content: String) {
        _uiState.update { it.copy(addDocContent = content) }
    }

    fun saveNewDocument() {
        val title = _uiState.value.addDocTitle.trim()
        val category = _uiState.value.addDocCategory.trim()
        val content = _uiState.value.addDocContent.trim()

        if (title.isEmpty() || content.isEmpty()) {
            _uiState.update { it.copy(feedbackMessage = "Title and Content cannot be empty") }
            return
        }

        viewModelScope.launch {
            try {
                repository.addDocument(title, category, content)
                _uiState.update {
                    it.copy(
                        showAddDialog = false,
                        feedbackMessage = "Document '$title' indexed into knowledge base!"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(feedbackMessage = "Failed to add document: ${e.localizedMessage}")
                }
            }
        }
    }

    fun deleteDocument(id: Long) {
        viewModelScope.launch {
            try {
                repository.deleteDocument(id)
                if (_uiState.value.inspectingDocument?.id == id) {
                    _uiState.update {
                        it.copy(inspectingDocument = null, inspectingDocumentChunks = emptyList())
                    }
                }
                _uiState.update { it.copy(feedbackMessage = "Document removed") }
            } catch (e: Exception) {
                _uiState.update { it.copy(feedbackMessage = "Could not delete document: ${e.localizedMessage}") }
            }
        }
    }

    fun resetSampleKnowledgeBase() {
        viewModelScope.launch {
            try {
                repository.resetToSampleDocuments()
                _uiState.update { it.copy(feedbackMessage = "Sample knowledge base reloaded!") }
            } catch (e: Exception) {
                _uiState.update { it.copy(feedbackMessage = "Reset failed: ${e.localizedMessage}") }
            }
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearChat()
        }
    }

    fun dismissFeedback() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }

    /**
     * Parses stored JSON sources into CitationSource objects.
     */
    fun parseSources(json: String?): List<CitationSource> {
        if (json.isNullOrBlank()) return emptyList()
        val list = mutableListOf<CitationSource>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    CitationSource(
                        documentId = obj.optLong("docId"),
                        documentTitle = obj.optString("docTitle"),
                        chunkIndex = obj.optInt("chunkIndex"),
                        textSnippet = obj.optString("snippet"),
                        relevanceScore = obj.optDouble("score", 90.0).toFloat()
                    )
                )
            }
        } catch (_: Exception) {
        }
        return list
    }
}
