# DocuQuery RAG — Document Question Answering System

DocuQuery RAG is a native Android assistant that performs question-answering over multi-page documents and text sources using a Retrieval-Augmented Generation (RAG) pipeline. Designed with a Bento Grid UI inspired by modern productivity suites, it enables users to index documents, search through chunked passages, and receive grounded answers with verifiable source citations and confidence scores.

---

## Features

- **Document & Knowledge Base Management**: Import, inspect, and manage enterprise and technical documents. View indexing status, page count, and document metadata at a glance.
- **Intelligent Text Chunking**: Boundary-aware sliding window chunking with sentence preservation, token count estimation, and keyword extraction.
- **Hybrid Retrieval Engine**:
  - **Vector Semantic Search**: Dense vector embeddings generated via `gemini-embedding-2-preview` with on-device cosine similarity calculation.
  - **BM25 Lexical Search**: Term frequency and inverse document frequency (TF-IDF/BM25) ranking for precise keyword matching.
  - **Hybrid Fusion**: Combines dense vector similarity and sparse BM25 scores for optimal retrieval precision and recall.
- **Grounded Answer Synthesis**: Powered by `gemini-3.5-flash` to synthesize answers exclusively from retrieved document chunks, preventing hallucinations and providing citation references.
- **Bento Grid UI**: Modular, adaptive Material 3 card layout displaying key metrics, indexed document cards, source inspector, confidence ratings, and suggested follow-up queries.
- **Local Persistence with Room**: Stores document records, chunks, embeddings, and chat history locally for instantaneous loading and offline inspection.

---

## Architecture Overview

DocuQuery follows modern Android architecture patterns (MVVM + Repository + Clean Architecture):

```
app/src/main/java/com/example/
├── MainActivity.kt                # Single activity entry point with edge-to-edge support
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt         # Room database configuration
│   │   ├── DocumentDao.kt         # DAOs for documents, chunks, and chat messages
│   │   ├── DocumentEntity.kt      # Schema definitions
│   │   └── SampleDocuments.kt    # Built-in knowledge base items
│   └── repository/
│       └── DocumentRepository.kt  # Single source of truth coordinating DB and RAG operations
├── rag/
│   ├── GeminiRagService.kt        # Gemini REST client for embeddings and generation
│   ├── Models.kt                  # RAG domain models (Citation, ChunkResult, RetrievalResult)
│   ├── RagRetriever.kt            # Hybrid vector + BM25 retriever
│   └── TextChunker.kt             # Semantic sliding-window chunker
└── ui/
    ├── DocQaScreen.kt             # Compose UI with Bento Grid tabs, chat, and inspector
    ├── RagUiState.kt              # Immutable UI state and navigation models
    ├── RagViewModel.kt            # Presentation logic and coroutine orchestration
    └── theme/
        ├── Color.kt               # Bento Grid palette and Material 3 color definitions
        ├── Theme.kt               # DocuQueryTheme with dynamic color controls
        └── Type.kt                # Typography configurations
```

---

## Getting Started

### Prerequisites

- **Android Studio**: Android Studio Ladybug (2024.2) or later
- **JDK**: Java Development Kit 17 or 21
- **Android SDK**: Compile SDK 36, Min SDK 24
- **Gemini API Key**: A valid Google AI Studio Gemini API key

### Clone and Open Project

```bash
git clone <repository-url>
cd <repository-directory>
```

Open the project directory in Android Studio. Gradle will automatically sync and resolve required dependencies.

### Configure API Key

1. Copy the sample environment file:
   ```bash
   cp .env.example .env
   ```
2. Edit `.env` and provide your Gemini API key:
   ```env
   GEMINI_API_KEY=your_gemini_api_key_here
   ```
   *(Note: The Secrets Gradle Plugin automatically reads `.env` and exposes `BuildConfig.GEMINI_API_KEY` at build time).*

### Build and Run

- **Via Android Studio**: Select `app` in the run configuration dropdown and click **Run** (`Shift + F10`) to deploy to a connected emulator or physical device.
- **Via Terminal**:
  ```bash
  gradle assembleDebug
  ```
  The generated APK will be located at `app/build/outputs/apk/debug/app-debug.apk`.

### Run Tests

Run JVM unit and Robolectric tests with:

```bash
gradle :app:testDebugUnitTest
```

---

## Tech Stack & Dependencies

- **Language**: Kotlin 2.x
- **UI Framework**: Jetpack Compose & Material 3
- **Architecture**: ViewModel, Kotlin Coroutines, StateFlow
- **Database**: AndroidX Room with KSP
- **Networking**: OkHttp 4
- **Testing**: JUnit 4, Robolectric, Roborazzi
- **Configuration**: Secrets Gradle Plugin

---

## License

This project is licensed under the Apache License 2.0.
