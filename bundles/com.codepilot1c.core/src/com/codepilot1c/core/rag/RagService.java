/*
 * Copyright (c) 2024 Example
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.codepilot1c.core.rag;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.codepilot1c.core.embedding.EmbeddingProviderRegistry;
import com.codepilot1c.core.embedding.IEmbeddingProvider;
import com.codepilot1c.core.index.ICodebaseIndex;
import com.codepilot1c.core.logging.LogSanitizer;
import com.codepilot1c.core.logging.VibeLogger;
import com.codepilot1c.core.rag.RagContextBuilder.RagContext;
import com.codepilot1c.core.search.ISemanticSearch;
import com.codepilot1c.core.search.ISemanticSearch.SearchResult;
import com.codepilot1c.core.search.SemanticSearchService;

/**
 * Central service for RAG (Retrieval-Augmented Generation) functionality.
 *
 * <p>Provides a unified interface for:</p>
 * <ul>
 *   <li>Codebase indexing</li>
 *   <li>Semantic search</li>
 *   <li>Context building for LLM prompts</li>
 * </ul>
 */
public class RagService {

    private static final VibeLogger.CategoryLogger LOG = VibeLogger.forClass(RagService.class);

    private static final String INDEX_DIR = ".vibe-index"; //$NON-NLS-1$
    private static final int DEFAULT_EMBEDDING_DIMENSION = 1536;
    private static final int DEFAULT_TOP_K = 5;
    private static final int DEFAULT_CONTEXT_TOKENS = 4000;

    private static RagService instance;

    private ICodebaseIndex codebaseIndex;
    private ISemanticSearch semanticSearch;
    private RagContextBuilder contextBuilder;
    private boolean indexingInProgress = false;

    private RagService() {
        this.contextBuilder = new RagContextBuilder(DEFAULT_CONTEXT_TOKENS);
    }

    /**
     * Returns the singleton instance.
     */
    public static synchronized RagService getInstance() {
        if (instance == null) {
            instance = new RagService();
        }
        return instance;
    }

    /**
     * Checks if RAG is ready (index exists and embedding provider is configured).
     */
    public boolean isReady() {
        return codebaseIndex != null
                && semanticSearch != null
                && semanticSearch.isReady();
    }

    /**
     * Checks if indexing is in progress.
     */
    public boolean isIndexingInProgress() {
        return indexingInProgress;
    }

    /**
     * Initializes the RAG service with the given workspace path.
     *
     * @param workspacePath the workspace root path
     * @return true if initialization succeeded
     */
    public boolean initialize(Path workspacePath) {
        long startTime = System.currentTimeMillis();
        LOG.info("Инициализация RAG сервиса, workspace: %s", workspacePath); //$NON-NLS-1$

        try {
            // Get embedding provider
            IEmbeddingProvider embeddingProvider = EmbeddingProviderRegistry.getInstance().getActiveProvider();
            if (embeddingProvider == null || !embeddingProvider.isConfigured()) {
                LOG.warn("Embedding провайдер не настроен для RAG"); //$NON-NLS-1$
                return false;
            }
            LOG.debug("Embedding провайдер: %s", embeddingProvider.getClass().getSimpleName()); //$NON-NLS-1$

            // Get or create index path
            Path indexPath = workspacePath.resolve(INDEX_DIR);
            LOG.debug("Путь к индексу: %s", indexPath); //$NON-NLS-1$

            // Create Lucene vector store
            // Note: This would need the actual LuceneVectorStore from rag bundle
            // For now, we'll check if it's already initialized
            if (codebaseIndex == null) {
                LOG.warn("RAG индекс ещё не создан"); //$NON-NLS-1$
                return false;
            }

            // Create semantic search service
            semanticSearch = new SemanticSearchService(codebaseIndex, embeddingProvider);

            long duration = System.currentTimeMillis() - startTime;
            LOG.info("RAG сервис инициализирован за %s", LogSanitizer.formatDuration(duration)); //$NON-NLS-1$
            return true;

        } catch (Exception e) {
            LOG.error("Ошибка инициализации RAG сервиса: %s", e.getMessage()); //$NON-NLS-1$
            return false;
        }
    }

    /**
     * Sets the codebase index (called by rag bundle after index is created).
     */
    public void setCodebaseIndex(ICodebaseIndex index) {
        LOG.info("Установка индекса кодовой базы: %s", //$NON-NLS-1$
                index != null ? index.getClass().getSimpleName() : "null"); //$NON-NLS-1$
        this.codebaseIndex = index;

        // Update semantic search if we have an embedding provider
        IEmbeddingProvider embeddingProvider = EmbeddingProviderRegistry.getInstance().getActiveProvider();
        if (embeddingProvider != null && embeddingProvider.isConfigured()) {
            semanticSearch = new SemanticSearchService(index, embeddingProvider);
            LOG.debug("Семантический поиск обновлён с провайдером: %s", //$NON-NLS-1$
                    embeddingProvider.getClass().getSimpleName());
        } else {
            LOG.warn("Embedding провайдер не настроен, семантический поиск недоступен"); //$NON-NLS-1$
        }
    }

    /**
     * Returns the codebase index.
     */
    public ICodebaseIndex getCodebaseIndex() {
        return codebaseIndex;
    }

    /**
     * Searches for relevant code based on a query.
     *
     * @param query the search query
     * @param topK number of results to return
     * @return list of search results
     */
    public List<SearchResult> search(String query, int topK) {
        if (!isReady()) {
            LOG.debug("Поиск невозможен: RAG не готов"); //$NON-NLS-1$
            return Collections.emptyList();
        }

        long startTime = System.currentTimeMillis();
        LOG.debug("Семантический поиск: query=%s, topK=%d", //$NON-NLS-1$
                LogSanitizer.truncate(query, 100), topK);

        List<SearchResult> results = semanticSearch.search(query, topK);

        long duration = System.currentTimeMillis() - startTime;
        LOG.debug("Поиск завершён: найдено %d результатов за %s", //$NON-NLS-1$
                results.size(), LogSanitizer.formatDuration(duration));

        return results;
    }

    /**
     * Searches asynchronously.
     */
    public CompletableFuture<List<SearchResult>> searchAsync(String query, int topK) {
        if (!isReady()) {
            LOG.debug("Асинхронный поиск невозможен: RAG не готов"); //$NON-NLS-1$
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        String correlationId = LogSanitizer.newCorrelationId();
        long startTime = System.currentTimeMillis();
        LOG.debug("[%s] Асинхронный поиск: query=%s, topK=%d", //$NON-NLS-1$
                correlationId, LogSanitizer.truncate(query, 100), topK);

        return semanticSearch.searchAsync(query, topK)
                .whenComplete((results, error) -> {
                    long duration = System.currentTimeMillis() - startTime;
                    if (error != null) {
                        LOG.error("[%s] Ошибка поиска за %s: %s", //$NON-NLS-1$
                                correlationId, LogSanitizer.formatDuration(duration), error.getMessage());
                    } else {
                        LOG.debug("[%s] Поиск завершён: %d результатов за %s", //$NON-NLS-1$
                                correlationId, results.size(), LogSanitizer.formatDuration(duration));
                    }
                });
    }

    /**
     * Builds RAG context for a user query.
     *
     * @param userQuery the user's question
     * @return the RAG context with relevant code
     */
    public RagContext buildContext(String userQuery) {
        return buildContext(userQuery, DEFAULT_TOP_K);
    }

    /**
     * Builds RAG context with specified number of results.
     *
     * @param userQuery the user's question
     * @param topK number of code chunks to include
     * @return the RAG context
     */
    public RagContext buildContext(String userQuery, int topK) {
        if (!isReady()) {
            LOG.debug("Построение контекста невозможно: RAG не готов"); //$NON-NLS-1$
            return new RagContext(userQuery, Collections.emptyList(), Collections.emptyList(), 0, 0, 0);
        }

        long startTime = System.currentTimeMillis();
        LOG.debug("Построение RAG контекста: topK=%d", topK); //$NON-NLS-1$

        List<SearchResult> results = search(userQuery, topK);
        RagContext context = contextBuilder.build(results, userQuery);

        long duration = System.currentTimeMillis() - startTime;
        LOG.info("RAG контекст построен: %d чанков, %d токенов за %s", //$NON-NLS-1$
                context.getRenderedChunks().size(),
                context.getTotalTokens(),
                LogSanitizer.formatDuration(duration));

        return context;
    }

    /**
     * Builds RAG context asynchronously.
     */
    public CompletableFuture<RagContext> buildContextAsync(String userQuery, int topK) {
        String correlationId = LogSanitizer.newCorrelationId();
        long startTime = System.currentTimeMillis();
        LOG.debug("[%s] Асинхронное построение RAG контекста: topK=%d", correlationId, topK); //$NON-NLS-1$

        return searchAsync(userQuery, topK)
                .thenApply(results -> {
                    RagContext context = contextBuilder.build(results, userQuery);
                    long duration = System.currentTimeMillis() - startTime;
                    LOG.info("[%s] RAG контекст построен: %d чанков, %d токенов за %s", //$NON-NLS-1$
                            correlationId,
                            context.getRenderedChunks().size(),
                            context.getTotalTokens(),
                            LogSanitizer.formatDuration(duration));
                    return context;
                });
    }

    /**
     * Returns the default index path for a workspace.
     */
    public static Path getDefaultIndexPath(Path workspacePath) {
        return workspacePath.resolve(INDEX_DIR);
    }

    /**
     * Returns the default embedding dimension.
     */
    public static int getDefaultEmbeddingDimension() {
        return DEFAULT_EMBEDDING_DIMENSION;
    }

    /**
     * Sets indexing in progress flag.
     */
    public void setIndexingInProgress(boolean inProgress) {
        if (this.indexingInProgress != inProgress) {
            LOG.info("Статус индексации: %s", inProgress ? "начата" : "завершена"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
        this.indexingInProgress = inProgress;
    }

    /**
     * Disposes the RAG service.
     */
    public void dispose() {
        LOG.info("Завершение работы RAG сервиса"); //$NON-NLS-1$
        if (codebaseIndex != null) {
            try {
                codebaseIndex.close();
                LOG.debug("Индекс кодовой базы закрыт"); //$NON-NLS-1$
            } catch (Exception e) {
                LOG.warn("Ошибка при закрытии индекса: %s", e.getMessage()); //$NON-NLS-1$
            }
            codebaseIndex = null;
        }
        semanticSearch = null;
        LOG.info("RAG сервис остановлен"); //$NON-NLS-1$
    }
}
