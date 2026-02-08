/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
 */
package com.codepilot1c.core.context7;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.codepilot1c.core.logging.VibeLogger;
import com.codepilot1c.core.model.LlmMessage;
import com.codepilot1c.core.model.LlmRequest;
import com.codepilot1c.core.model.LlmResponse;
import com.codepilot1c.core.provider.ILlmProvider;

/**
 * LLM-based query classifier for intelligent documentation routing.
 *
 * <p>Uses a fast LLM call to classify user queries into categories:</p>
 * <ul>
 *   <li>SYNTAX - BSL syntax, data types, functions</li>
 *   <li>DIAGNOSTICS - code quality, linting, diagnostics</li>
 *   <li>SUBSYSTEMS - BSP, standard subsystems, common modules</li>
 *   <li>GENERAL - general 1C questions requiring documentation</li>
 *   <li>NONE - no documentation needed</li>
 * </ul>
 */
public class QueryClassifier {

    private static final VibeLogger.CategoryLogger LOG =
            VibeLogger.forClass(QueryClassifier.class);

    /** Singleton instance (volatile for thread-safe lazy initialization) */
    private static volatile QueryClassifier instance;

    /** Classification prompt template */
    private static final String CLASSIFICATION_PROMPT = """
        Классифицируй запрос пользователя для 1С:Предприятие.
        Ответь ТОЛЬКО ОДНИМ словом из списка:

        - SYNTAX - вопросы о синтаксисе BSL, типах данных, функциях, операторах, примерах кода
        - DIAGNOSTICS - вопросы о диагностиках, качестве кода, линтинге, анализе, ошибках компиляции
        - SUBSYSTEMS - вопросы о БСП (библиотека стандартных подсистем), общих модулях, обмене данными
        - GENERAL - общие вопросы о 1С, метаданных, справочниках, документах, регистрах
        - NONE - не требует документации (приветствие, благодарность, вопросы не о 1С)

        Запрос: %s

        Категория:""";

    /** Cache for classification results */
    private final Map<String, CacheEntry> classificationCache = new ConcurrentHashMap<>();

    /** Cache TTL in milliseconds (30 minutes) */
    private static final long CACHE_TTL_MS = 30 * 60 * 1000;

    /** Maximum cache size */
    private static final int MAX_CACHE_SIZE = 200;

    /** Timeout for classification request in seconds */
    private static final int CLASSIFICATION_TIMEOUT_SECONDS = 20;

    /** Flag indicating if warmup was performed */
    private volatile boolean warmedUp = false;

    /** LLM provider for classification */
    private ILlmProvider llmProvider;

    /** Cache entry with timestamp */
    private static class CacheEntry {
        final Context7DocumentationService.QueryCategory category;
        final Instant createdAt;

        CacheEntry(Context7DocumentationService.QueryCategory category) {
            this.category = category;
            this.createdAt = Instant.now();
        }

        boolean isExpired() {
            return Duration.between(createdAt, Instant.now()).toMillis() > CACHE_TTL_MS;
        }
    }

    private QueryClassifier() {
        // Private constructor for singleton
    }

    /**
     * Gets singleton instance.
     */
    public static synchronized QueryClassifier getInstance() {
        if (instance == null) {
            instance = new QueryClassifier();
        }
        return instance;
    }

    /**
     * Disposes the singleton instance (for plugin shutdown).
     * Releases LLM provider reference and clears cache.
     */
    public static synchronized void dispose() {
        if (instance != null) {
            instance.llmProvider = null;
            instance.classificationCache.clear();
            instance.warmedUp = false;
            LOG.info("QueryClassifier disposed");
            instance = null;
        }
    }

    /**
     * Sets the LLM provider to use for classification.
     *
     * @param provider the LLM provider
     */
    public void setLlmProvider(ILlmProvider provider) {
        this.llmProvider = provider;
        LOG.info("QueryClassifier: LLM provider set to %s",
                provider != null ? provider.getClass().getSimpleName() : "null");

        // Perform warmup asynchronously
        if (provider != null && !warmedUp) {
            warmup();
        }
    }

    /**
     * Performs warmup by sending a simple classification request.
     * This helps avoid cold start delays on the first real request.
     */
    private void warmup() {
        if (llmProvider == null || warmedUp) {
            return;
        }

        LOG.info("QueryClassifier: starting warmup...");

        CompletableFuture.runAsync(() -> {
            try {
                // Simple warmup query
                String warmupQuery = "Привет";
                LlmMessage systemMessage = LlmMessage.system(
                    "Ответь одним словом: NONE"
                );
                LlmMessage userMessage = LlmMessage.user(warmupQuery);

                LlmRequest request = LlmRequest.builder()
                        .messages(List.of(systemMessage, userMessage))
                        .maxTokens(5)
                        .temperature(0.0)
                        .build();

                llmProvider.complete(request)
                    .orTimeout(15, TimeUnit.SECONDS)
                    .thenAccept(response -> {
                        warmedUp = true;
                        LOG.info("QueryClassifier: warmup completed successfully");
                    })
                    .exceptionally(e -> {
                        warmedUp = true; // Mark as warmed up even on failure
                        LOG.warn("QueryClassifier: warmup failed: %s", e.getMessage());
                        return null;
                    });
            } catch (Exception e) {
                warmedUp = true;
                LOG.warn("QueryClassifier: warmup exception: %s", e.getMessage());
            }
        });
    }

    /**
     * Classifies a user query using LLM.
     *
     * @param userQuery the user's query
     * @return CompletableFuture with the classification result
     */
    public CompletableFuture<Context7DocumentationService.QueryCategory> classify(String userQuery) {
        if (userQuery == null || userQuery.trim().isEmpty()) {
            return CompletableFuture.completedFuture(Context7DocumentationService.QueryCategory.GENERAL);
        }

        // Normalize query for cache key
        String cacheKey = normalizeForCache(userQuery);

        // Check cache
        CacheEntry cached = classificationCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            LOG.debug("Classification cache hit: %s -> %s",
                    truncate(userQuery, 50), cached.category);
            return CompletableFuture.completedFuture(cached.category);
        }

        // Check if LLM provider is available
        if (llmProvider == null) {
            LOG.warn("No LLM provider available for classification, using GENERAL");
            return CompletableFuture.completedFuture(Context7DocumentationService.QueryCategory.GENERAL);
        }

        LOG.debug("Classifying query with LLM: %s", truncate(userQuery, 50));

        // Build classification request
        String prompt = String.format(CLASSIFICATION_PROMPT, userQuery);
        LlmMessage systemMessage = LlmMessage.system(
            "Ты классификатор запросов. Отвечай только одним словом: SYNTAX, DIAGNOSTICS, SUBSYSTEMS, GENERAL или NONE."
        );
        LlmMessage userMessage = LlmMessage.user(prompt);

        LlmRequest request = LlmRequest.builder()
                .messages(List.of(systemMessage, userMessage))
                .maxTokens(10) // We only need one word
                .temperature(0.0) // Deterministic response
                .build();

        return llmProvider.complete(request)
                .orTimeout(CLASSIFICATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .thenApply(response -> {
                    Context7DocumentationService.QueryCategory category = parseCategory(response);

                    // Cache the result
                    putInCache(cacheKey, category);

                    LOG.info("Query classified: '%s' -> %s", truncate(userQuery, 50), category);
                    return category;
                })
                .exceptionally(e -> {
                    LOG.warn("Classification failed, using GENERAL: %s", e.getMessage());
                    return Context7DocumentationService.QueryCategory.GENERAL;
                });
    }

    /**
     * Parses the LLM response to extract category.
     */
    private Context7DocumentationService.QueryCategory parseCategory(LlmResponse response) {
        if (response == null || response.getContent() == null) {
            return Context7DocumentationService.QueryCategory.GENERAL;
        }

        String content = response.getContent().trim().toUpperCase();

        // Extract the category word (handle potential extra text)
        if (content.contains("NONE")) {
            return null; // Special case: no documentation needed
        }
        if (content.contains("SYNTAX")) {
            return Context7DocumentationService.QueryCategory.SYNTAX;
        }
        if (content.contains("DIAGNOSTICS")) {
            return Context7DocumentationService.QueryCategory.DIAGNOSTICS;
        }
        if (content.contains("SUBSYSTEMS")) {
            return Context7DocumentationService.QueryCategory.SUBSYSTEMS;
        }
        if (content.contains("GENERAL")) {
            return Context7DocumentationService.QueryCategory.GENERAL;
        }

        // Default to GENERAL if parsing fails
        LOG.debug("Could not parse category from: %s, using GENERAL", content);
        return Context7DocumentationService.QueryCategory.GENERAL;
    }

    /**
     * Normalizes query for cache key.
     */
    private String normalizeForCache(String query) {
        return query.toLowerCase()
                .replaceAll("[?!.,;:]+", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * Puts classification in cache.
     */
    private void putInCache(String key, Context7DocumentationService.QueryCategory category) {
        if (category == null) {
            return; // Don't cache NONE results
        }

        // Clean expired entries if cache is full
        if (classificationCache.size() >= MAX_CACHE_SIZE) {
            cleanExpiredCache();
        }

        classificationCache.put(key, new CacheEntry(category));
    }

    /**
     * Cleans expired cache entries (thread-safe implementation).
     */
    private void cleanExpiredCache() {
        int removed = 0;
        for (var entry : classificationCache.entrySet()) {
            if (entry.getValue().isExpired()) {
                // Atomic remove only if value hasn't changed
                if (classificationCache.remove(entry.getKey(), entry.getValue())) {
                    removed++;
                }
            }
        }
        if (removed > 0) {
            LOG.debug("Cleaned %d expired classification cache entries", removed);
        }
    }

    /**
     * Clears the classification cache.
     */
    public void clearCache() {
        classificationCache.clear();
        LOG.info("Classification cache cleared");
    }

    /**
     * Truncates string for logging.
     */
    private String truncate(String s, int maxLen) {
        if (s == null) return "null";
        return s.length() > maxLen ? s.substring(0, maxLen) + "..." : s;
    }
}
