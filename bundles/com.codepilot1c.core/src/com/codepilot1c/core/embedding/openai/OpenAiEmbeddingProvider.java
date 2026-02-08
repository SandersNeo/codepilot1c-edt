/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
 */
package com.codepilot1c.core.embedding.openai;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;

import com.codepilot1c.core.embedding.EmbeddingProviderException;
import com.codepilot1c.core.embedding.EmbeddingResult;
import com.codepilot1c.core.embedding.IEmbeddingProvider;
import com.codepilot1c.core.http.HttpClientFactory;
import com.codepilot1c.core.http.HttpRequestBodies;
import com.codepilot1c.core.internal.VibeCorePlugin;
import com.codepilot1c.core.logging.LogSanitizer;
import com.codepilot1c.core.logging.VibeLogger;
import com.codepilot1c.core.settings.VibePreferenceConstants;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * OpenAI embeddings provider implementation.
 *
 * <p>Uses the OpenAI /v1/embeddings API endpoint.</p>
 */
public class OpenAiEmbeddingProvider implements IEmbeddingProvider {

    private static final VibeLogger.CategoryLogger LOG = VibeLogger.forClass(OpenAiEmbeddingProvider.class);

    private static final String DEFAULT_MODEL = "text-embedding-3-small"; //$NON-NLS-1$
    private static final int DEFAULT_DIMENSIONS = 1536;
    private static final int MAX_CHARS_PER_TEXT = 8000; // ~2000 tokens, safe limit
    private static final int MAX_TEXTS_PER_REQUEST = 5; // Small batch to avoid 413

    // Parallelism and retry settings
    private static final int MAX_CONCURRENT_REQUESTS = 3;
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 1000;
    private static final long MAX_BACKOFF_MS = 30000;

    private final Gson gson = new GsonBuilder().create();
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final Semaphore requestSemaphore = new Semaphore(MAX_CONCURRENT_REQUESTS);
    // Dedicated executor for embedding tasks - avoids ForkJoinPool starvation
    private final ExecutorService embeddingExecutor = Executors.newFixedThreadPool(
            MAX_CONCURRENT_REQUESTS + 2, // Extra threads for scheduling
            r -> {
                Thread t = new Thread(r, "embedding-worker"); //$NON-NLS-1$
                t.setDaemon(true);
                return t;
            });

    @Override
    public String getId() {
        return "openai"; //$NON-NLS-1$
    }

    @Override
    public String getDisplayName() {
        return "OpenAI Embeddings"; //$NON-NLS-1$
    }

    private static final String DEFAULT_API_URL = "https://api.openai.com/v1"; //$NON-NLS-1$

    @Override
    public boolean isConfigured() {
        String apiUrl = getApiUrl();
        String apiKey = getApiKey();

        // Custom OpenAI-compatible endpoints may not require API key
        // If user explicitly set a custom URL, consider it configured
        boolean hasCustomUrl = apiUrl != null && !apiUrl.isEmpty()
                && !apiUrl.equals(DEFAULT_API_URL);

        if (hasCustomUrl) {
            return true;
        }

        // For default OpenAI, API key is required
        return apiKey != null && !apiKey.isEmpty();
    }

    @Override
    public int getDimensions() {
        String model = getModel();
        // Qwen text-embedding-v3/v4: 1024
        if (model.contains("text-embedding-v")) { //$NON-NLS-1$
            return 1024;
        }
        // text-embedding-3-large: 3072
        if (model.contains("large")) { //$NON-NLS-1$
            return 3072;
        }
        // text-embedding-3-small, text-embedding-ada-002: 1536
        return DEFAULT_DIMENSIONS;
    }

    @Override
    public int getMaxBatchSize() {
        return getPreferences().getInt(VibePreferenceConstants.PREF_EMBEDDING_BATCH_SIZE, 100);
    }

    @Override
    public int getMaxTokens() {
        return 8191;
    }

    private IEclipsePreferences getPreferences() {
        return InstanceScope.INSTANCE.getNode(VibeCorePlugin.PLUGIN_ID);
    }

    private String getApiKey() {
        return getPreferences().get(VibePreferenceConstants.PREF_EMBEDDING_API_KEY, ""); //$NON-NLS-1$
    }

    private String getApiUrl() {
        return getPreferences().get(VibePreferenceConstants.PREF_EMBEDDING_API_URL,
                "https://api.openai.com/v1"); //$NON-NLS-1$
    }

    private String getModel() {
        return getPreferences().get(VibePreferenceConstants.PREF_OPENAI_EMBEDDING_MODEL, DEFAULT_MODEL);
    }

    /**
     * Returns the shared HTTP client from the factory.
     *
     * @return the HTTP client
     */
    private HttpClient getHttpClient() {
        HttpClientFactory factory = VibeCorePlugin.getDefault().getHttpClientFactory();
        if (factory != null) {
            return factory.getSharedClient();
        }
        // Fallback if plugin not started (e.g., testing)
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    @Override
    public CompletableFuture<EmbeddingResult> embed(String text) {
        cancelled.set(false);

        if (!isConfigured()) {
            return CompletableFuture.failedFuture(
                    new EmbeddingProviderException("Embedding provider is not configured")); //$NON-NLS-1$
        }

        return embedBatch(List.of(text)).thenApply(results -> {
            if (results.isEmpty()) {
                throw new EmbeddingProviderException("No embedding result returned"); //$NON-NLS-1$
            }
            return results.get(0);
        });
    }

    @Override
    public CompletableFuture<List<EmbeddingResult>> embedBatch(List<String> texts) {
        cancelled.set(false);

        if (!isConfigured()) {
            LOG.warn("Embedding провайдер не настроен"); //$NON-NLS-1$
            return CompletableFuture.failedFuture(
                    new EmbeddingProviderException("Embedding provider is not configured")); //$NON-NLS-1$
        }

        if (texts.isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
        }

        // Clean and truncate texts (API returns 400 for empty/null strings)
        List<String> cleanedTexts = new ArrayList<>();
        int emptyCount = 0;
        for (int i = 0; i < texts.size(); i++) {
            String text = texts.get(i);

            // Handle null or empty strings - use placeholder to maintain indexing
            if (text == null || text.trim().isEmpty()) {
                cleanedTexts.add("[пусто]"); // Placeholder for empty content //$NON-NLS-1$
                emptyCount++;
                continue;
            }

            // Clean text: remove null bytes and other problematic characters
            String cleaned = text.replace("\u0000", "") //$NON-NLS-1$ //$NON-NLS-2$
                    .replace("\uFFFD", ""); //$NON-NLS-1$ //$NON-NLS-2$

            if (cleaned.trim().isEmpty()) {
                cleanedTexts.add("[пусто]"); // Placeholder //$NON-NLS-1$
                emptyCount++;
                continue;
            }

            // Truncate if too long
            if (cleaned.length() > MAX_CHARS_PER_TEXT) {
                cleaned = cleaned.substring(0, MAX_CHARS_PER_TEXT);
            }

            cleanedTexts.add(cleaned);
        }

        if (emptyCount > 0) {
            LOG.warn("Заменено %d пустых текстов на placeholder", emptyCount); //$NON-NLS-1$
        }

        int totalChars = cleanedTexts.stream().mapToInt(String::length).sum();
        LOG.debug("Запрос embeddings: %d текстов, %d символов, модель=%s", //$NON-NLS-1$
                cleanedTexts.size(), totalChars, getModel());

        // Split into smaller batches to avoid 413 errors
        if (cleanedTexts.size() > MAX_TEXTS_PER_REQUEST) {
            return embedInBatches(cleanedTexts);
        }

        return embedSingleBatch(cleanedTexts);
    }

    /**
     * Embeds texts in multiple smaller batches with controlled parallelism.
     *
     * <p>Uses semaphore to limit concurrent API requests and processes batches
     * with retry logic for transient errors.</p>
     */
    private CompletableFuture<List<EmbeddingResult>> embedInBatches(List<String> texts) {
        // Create batch list
        List<List<String>> batches = new ArrayList<>();
        for (int i = 0; i < texts.size(); i += MAX_TEXTS_PER_REQUEST) {
            int end = Math.min(i + MAX_TEXTS_PER_REQUEST, texts.size());
            batches.add(new ArrayList<>(texts.subList(i, end)));
        }

        LOG.debug("Разделение на %d батчей (по %d текстов), параллелизм=%d", //$NON-NLS-1$
                batches.size(), MAX_TEXTS_PER_REQUEST, MAX_CONCURRENT_REQUESTS);

        // Process all batches with controlled parallelism
        List<CompletableFuture<List<EmbeddingResult>>> futures = new ArrayList<>();
        int cancelledFromBatch = -1;
        for (int i = 0; i < batches.size(); i++) {
            if (cancelled.get()) {
                LOG.warn("Отмена: пропущено %d батчей", batches.size() - i); //$NON-NLS-1$
                cancelledFromBatch = i;
                break;
            }
            final int batchIndex = i;
            final List<String> batch = batches.get(i);

            // Create future that acquires semaphore before making request
            // Use dedicated executor to avoid ForkJoinPool starvation with blocking semaphore
            CompletableFuture<List<EmbeddingResult>> future = CompletableFuture.supplyAsync(() -> {
                try {
                    // Acquire semaphore permit (blocks if MAX_CONCURRENT_REQUESTS already running)
                    // Use long timeout since each batch can take 60+ seconds on slow servers
                    if (!requestSemaphore.tryAcquire(10, TimeUnit.MINUTES)) {
                        throw new EmbeddingProviderException("Таймаут ожидания семафора для batch " + batchIndex); //$NON-NLS-1$
                    }
                    try {
                        return embedSingleBatchWithRetry(batch, batchIndex).join();
                    } finally {
                        requestSemaphore.release();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new EmbeddingProviderException("Прервано ожидание семафора", e); //$NON-NLS-1$
                }
            }, embeddingExecutor); // Use dedicated executor
            futures.add(future);
        }

        // Combine all futures - use handle() to process results even if some batches fail
        final int finalCancelledFromBatch = cancelledFromBatch;
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .handle((v, allOfError) -> {
                    // handle() runs regardless of success/failure, unlike thenApply()
                    List<EmbeddingResult> results = new ArrayList<>();
                    int index = 0;

                    // Process completed futures (some may have failed)
                    for (int i = 0; i < futures.size(); i++) {
                        CompletableFuture<List<EmbeddingResult>> future = futures.get(i);
                        try {
                            for (EmbeddingResult result : future.join()) {
                                // Reindex to match original order
                                results.add(new EmbeddingResult(result.getEmbedding(), index++, result.getTokensUsed()));
                            }
                        } catch (Exception e) {
                            LOG.error("Ошибка в batch %d: %s", i, e.getMessage()); //$NON-NLS-1$
                            // Add empty embeddings for failed batch
                            int batchSize = i < batches.size() ? batches.get(i).size() : MAX_TEXTS_PER_REQUEST;
                            for (int j = 0; j < batchSize && index < texts.size(); j++) {
                                results.add(new EmbeddingResult(new float[getDimensions()], index++, 0));
                            }
                        }
                    }

                    // Add empty placeholders for cancelled batches to maintain alignment
                    if (finalCancelledFromBatch >= 0) {
                        for (int i = finalCancelledFromBatch; i < batches.size(); i++) {
                            int batchSize = batches.get(i).size();
                            for (int j = 0; j < batchSize; j++) {
                                results.add(new EmbeddingResult(new float[getDimensions()], index++, 0));
                            }
                        }
                        LOG.info("Добавлено %d пустых embeddings для %d отменённых батчей", //$NON-NLS-1$
                                texts.size() - futures.size() * MAX_TEXTS_PER_REQUEST, batches.size() - finalCancelledFromBatch);
                    }

                    return results;
                });
    }

    /**
     * Embeds a single batch with retry logic for transient errors.
     *
     * <p>Retries on:
     * <ul>
     *   <li>HTTP 429 (Rate limit)</li>
     *   <li>HTTP 5xx (Server errors)</li>
     *   <li>Timeouts</li>
     * </ul>
     * Uses exponential backoff between retries.</p>
     *
     * @param texts the texts to embed
     * @param batchIndex the batch index for logging
     * @return future with embedding results
     */
    private CompletableFuture<List<EmbeddingResult>> embedSingleBatchWithRetry(List<String> texts, int batchIndex) {
        return embedWithRetry(texts, batchIndex, 0, INITIAL_BACKOFF_MS);
    }

    /**
     * Recursive retry implementation with exponential backoff.
     */
    private CompletableFuture<List<EmbeddingResult>> embedWithRetry(
            List<String> texts, int batchIndex, int attempt, long backoffMs) {

        if (cancelled.get()) {
            return CompletableFuture.failedFuture(
                    new EmbeddingProviderException("Операция отменена")); //$NON-NLS-1$
        }

        return embedSingleBatch(texts)
                .exceptionallyCompose(error -> {
                    // Check if we should retry
                    if (attempt >= MAX_RETRIES) {
                        LOG.error("Batch %d: исчерпаны попытки (%d), ошибка: %s", //$NON-NLS-1$
                                batchIndex, MAX_RETRIES, error.getMessage());
                        return CompletableFuture.failedFuture(error);
                    }

                    if (cancelled.get()) {
                        return CompletableFuture.failedFuture(error);
                    }

                    // Check if error is retryable
                    boolean shouldRetry = isRetryableError(error);
                    if (!shouldRetry) {
                        LOG.error("Batch %d: неповторяемая ошибка: %s", batchIndex, error.getMessage()); //$NON-NLS-1$
                        return CompletableFuture.failedFuture(error);
                    }

                    // Calculate backoff with jitter
                    long jitter = (long) (Math.random() * backoffMs * 0.2);
                    long actualBackoff = Math.min(backoffMs + jitter, MAX_BACKOFF_MS);

                    LOG.warn("Batch %d: retry %d/%d через %dмс, ошибка: %s", //$NON-NLS-1$
                            batchIndex, attempt + 1, MAX_RETRIES, actualBackoff, error.getMessage());

                    // Schedule retry after backoff using dedicated executor
                    return CompletableFuture.supplyAsync(() -> {
                        try {
                            Thread.sleep(actualBackoff);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new EmbeddingProviderException("Retry прерван", e); //$NON-NLS-1$
                        }
                        return null;
                    }, embeddingExecutor).thenCompose(v -> embedWithRetry(texts, batchIndex, attempt + 1, backoffMs * 2));
                });
    }

    /**
     * Checks if an error is retryable.
     *
     * @param error the error to check
     * @return true if the error is transient and worth retrying
     */
    private boolean isRetryableError(Throwable error) {
        // Unwrap CompletionException
        Throwable cause = error;
        while (cause.getCause() != null && cause != cause.getCause()) {
            cause = cause.getCause();
        }

        // Timeout is retryable
        if (cause instanceof HttpTimeoutException) {
            return true;
        }

        // Check HTTP status code in our exception
        if (cause instanceof EmbeddingProviderException) {
            EmbeddingProviderException epe = (EmbeddingProviderException) cause;
            int statusCode = epe.getStatusCode();

            // 429 = Rate limit exceeded
            if (statusCode == 429) {
                return true;
            }

            // 5xx = Server errors
            if (statusCode >= 500 && statusCode < 600) {
                return true;
            }

            // 408 = Request timeout
            if (statusCode == 408) {
                return true;
            }
        }

        // Connection errors are also retryable
        if (cause instanceof java.net.ConnectException
                || cause instanceof java.net.SocketTimeoutException
                || cause instanceof java.io.IOException && cause.getMessage() != null
                        && cause.getMessage().contains("Connection reset")) { //$NON-NLS-1$
            return true;
        }

        return false;
    }

    /**
     * Embeds a single batch of texts.
     */
    private CompletableFuture<List<EmbeddingResult>> embedSingleBatch(List<String> texts) {
        String requestBody = buildRequestBody(texts);
        long startTime = System.currentTimeMillis();

        // Build request with GZIP compression support
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(getApiUrl() + "/embeddings")) //$NON-NLS-1$
                .timeout(Duration.ofMinutes(3)); // Allow time for slow APIs like Qwen

        // Add Authorization header only if API key is provided
        String apiKey = getApiKey();
        if (apiKey != null && !apiKey.isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + apiKey); //$NON-NLS-1$ //$NON-NLS-2$
        }

        // Don't use GZIP compression for embedding API - many servers don't support it
        // (e.g., text-embeddings-inference returns "Failed to parse request body as JSON")
        HttpRequestBodies.postJsonUncompressed(requestBuilder, requestBody);

        HttpRequest request = requestBuilder.build();

        return getHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    long duration = System.currentTimeMillis() - startTime;
                    LOG.debug("Embeddings получены за %s, HTTP status=%d", //$NON-NLS-1$
                            LogSanitizer.formatDuration(duration), response.statusCode());
                    return parseResponse(response);
                })
                .whenComplete((result, error) -> {
                    if (error != null) {
                        LOG.error("Ошибка получения embeddings: %s", error.getMessage()); //$NON-NLS-1$
                    }
                });
    }

    private String buildRequestBody(List<String> texts) {
        JsonObject body = new JsonObject();
        body.addProperty("model", getModel()); //$NON-NLS-1$

        JsonArray input = new JsonArray();
        for (String text : texts) {
            input.add(text);
        }
        body.add("input", input); //$NON-NLS-1$

        return gson.toJson(body);
    }

    private List<EmbeddingResult> parseResponse(HttpResponse<String> response) {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw parseError(response);
        }

        try {
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonArray data = json.getAsJsonArray("data"); //$NON-NLS-1$

            List<EmbeddingResult> results = new ArrayList<>();
            int totalTokens = 0;

            if (json.has("usage")) { //$NON-NLS-1$
                JsonObject usage = json.getAsJsonObject("usage"); //$NON-NLS-1$
                totalTokens = usage.get("total_tokens").getAsInt(); //$NON-NLS-1$
            }

            for (int i = 0; i < data.size(); i++) {
                JsonObject item = data.get(i).getAsJsonObject();
                JsonArray embeddingArray = item.getAsJsonArray("embedding"); //$NON-NLS-1$
                int index = item.get("index").getAsInt(); //$NON-NLS-1$

                float[] embedding = new float[embeddingArray.size()];
                for (int j = 0; j < embeddingArray.size(); j++) {
                    embedding[j] = embeddingArray.get(j).getAsFloat();
                }

                results.add(new EmbeddingResult(embedding, index, totalTokens / Math.max(1, data.size())));
            }

            return results;
        } catch (EmbeddingProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new EmbeddingProviderException("Failed to parse OpenAI embedding response", e); //$NON-NLS-1$
        }
    }

    private EmbeddingProviderException parseError(HttpResponse<String> response) {
        String body = response.body();
        LOG.error("API error %d, body: %s", response.statusCode(), //$NON-NLS-1$
                body != null && body.length() > 500 ? body.substring(0, 500) + "..." : body); //$NON-NLS-1$

        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            if (json.has("error")) { //$NON-NLS-1$
                JsonObject error = json.getAsJsonObject("error"); //$NON-NLS-1$
                String type = error.has("type") ? error.get("type").getAsString() : null; //$NON-NLS-1$ //$NON-NLS-2$
                String message = error.has("message") ? error.get("message").getAsString() //$NON-NLS-1$ //$NON-NLS-2$
                        : "Unknown error"; //$NON-NLS-1$
                LOG.error("API error message: %s, type: %s", message, type); //$NON-NLS-1$
                return new EmbeddingProviderException(message, null, response.statusCode(), type);
            }
            // Try alternate error format (some APIs use "detail" instead of "error")
            if (json.has("detail")) { //$NON-NLS-1$
                String detail = json.get("detail").getAsString(); //$NON-NLS-1$
                LOG.error("API error detail: %s", detail); //$NON-NLS-1$
                return new EmbeddingProviderException(detail, null, response.statusCode(), null);
            }
        } catch (Exception e) {
            LOG.error("Не удалось распарсить ответ об ошибке: %s", e.getMessage()); //$NON-NLS-1$
        }
        return new EmbeddingProviderException(
                "OpenAI embedding API error: " + response.statusCode() + " - " + body, null, response.statusCode(), null); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    public void cancel() {
        cancelled.set(true);
    }

    @Override
    public void dispose() {
        cancel();
        // Shutdown the embedding executor
        embeddingExecutor.shutdownNow();
        // HTTP client is managed by HttpClientFactory, no cleanup needed here
    }
}
