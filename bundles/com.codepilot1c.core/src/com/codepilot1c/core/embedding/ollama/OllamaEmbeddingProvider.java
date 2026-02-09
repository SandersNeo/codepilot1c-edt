/*
 * Copyright (c) 2024 Example
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.codepilot1c.core.embedding.ollama;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;

import com.codepilot1c.core.embedding.EmbeddingProviderException;
import com.codepilot1c.core.embedding.EmbeddingResult;
import com.codepilot1c.core.embedding.IEmbeddingProvider;
import com.codepilot1c.core.http.HttpClientFactory;
import com.codepilot1c.core.internal.VibeCorePlugin;
import com.codepilot1c.core.settings.VibePreferenceConstants;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Ollama embeddings provider implementation for local embedding generation.
 *
 * <p>Uses the Ollama /api/embeddings endpoint. Supports models like nomic-embed-text,
 * all-minilm, mxbai-embed-large, etc.</p>
 *
 * <p>This provider enables zero-config embedding when Ollama is running locally,
 * removing the need for API keys or cloud services.</p>
 */
public class OllamaEmbeddingProvider implements IEmbeddingProvider {

    /** Default Ollama API URL. */
    public static final String DEFAULT_API_URL = "http://localhost:11434"; //$NON-NLS-1$

    /** Default embedding model optimized for code. */
    public static final String DEFAULT_MODEL = "nomic-embed-text"; //$NON-NLS-1$

    /** Embedding dimension for nomic-embed-text model. */
    private static final int NOMIC_DIMENSIONS = 768;

    private final Gson gson = new GsonBuilder().create();
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    /** Cached availability status. */
    private volatile Boolean availabilityCache = null;
    private volatile long availabilityCacheTime = 0;
    private static final long AVAILABILITY_CACHE_TTL = 30_000; // 30 seconds

    @Override
    public String getId() {
        return "ollama"; //$NON-NLS-1$
    }

    @Override
    public String getDisplayName() {
        return "Ollama Embeddings (Local)"; //$NON-NLS-1$
    }

    @Override
    public boolean isConfigured() {
        // Check cached availability
        long now = System.currentTimeMillis();
        if (availabilityCache != null && (now - availabilityCacheTime) < AVAILABILITY_CACHE_TTL) {
            return availabilityCache;
        }

        // Check if Ollama is running and has the required model
        boolean available = checkOllamaAvailability();
        availabilityCache = available;
        availabilityCacheTime = now;
        return available;
    }

    /**
     * Checks if Ollama is running and has the embedding model available.
     *
     * @return true if Ollama is available and ready
     */
    private boolean checkOllamaAvailability() {
        try {
            HttpClient client = getHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(getApiUrl() + "/api/tags")) //$NON-NLS-1$
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return false;
            }

            // Check if the required model is available
            String model = getModel();
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            if (json.has("models")) { //$NON-NLS-1$
                JsonArray models = json.getAsJsonArray("models"); //$NON-NLS-1$
                for (int i = 0; i < models.size(); i++) {
                    JsonObject modelObj = models.get(i).getAsJsonObject();
                    String name = modelObj.get("name").getAsString(); //$NON-NLS-1$
                    // Model name can include tag (e.g., "nomic-embed-text:latest")
                    if (name.equals(model) || name.startsWith(model + ":")) { //$NON-NLS-1$
                        return true;
                    }
                }
            }

            VibeCorePlugin.logInfo("Ollama available but model '" + model + "' not found. " //$NON-NLS-1$ //$NON-NLS-2$
                    + "Run 'ollama pull " + model + "' to install."); //$NON-NLS-1$ //$NON-NLS-2$
            return false;

        } catch (Exception e) {
            // Ollama not running or network error
            return false;
        }
    }

    /**
     * Invalidates the availability cache, forcing a fresh check.
     */
    public void invalidateAvailabilityCache() {
        availabilityCache = null;
        availabilityCacheTime = 0;
    }

    @Override
    public int getDimensions() {
        String model = getModel();
        // Return dimensions based on known models
        if (model.contains("nomic-embed-text")) { //$NON-NLS-1$
            return NOMIC_DIMENSIONS;
        } else if (model.contains("all-minilm") || model.contains("bge-small")) { //$NON-NLS-1$ //$NON-NLS-2$
            return 384;
        } else if (model.contains("mxbai-embed-large")) { //$NON-NLS-1$
            return 1024;
        }
        // Default to nomic dimensions
        return NOMIC_DIMENSIONS;
    }

    @Override
    public int getMaxBatchSize() {
        // Ollama processes one embedding at a time, batch sequentially
        return 1;
    }

    @Override
    public int getMaxTokens() {
        // nomic-embed-text supports 8192 tokens context
        return 8192;
    }

    private IEclipsePreferences getPreferences() {
        return InstanceScope.INSTANCE.getNode(VibeCorePlugin.PLUGIN_ID);
    }

    private String getApiUrl() {
        String url = getPreferences().get(VibePreferenceConstants.PREF_OLLAMA_EMBEDDING_API_URL, ""); //$NON-NLS-1$
        if (url.isEmpty()) {
            // Fall back to main Ollama URL if embedding-specific URL not set
            url = getPreferences().get(VibePreferenceConstants.PREF_OLLAMA_API_URL, DEFAULT_API_URL);
        }
        // Remove trailing slash
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url; //$NON-NLS-1$
    }

    private String getModel() {
        return getPreferences().get(VibePreferenceConstants.PREF_OLLAMA_EMBEDDING_MODEL, DEFAULT_MODEL);
    }

    private HttpClient getHttpClient() {
        HttpClientFactory factory = VibeCorePlugin.getDefault().getHttpClientFactory();
        if (factory != null) {
            return factory.getSharedClient();
        }
        // Fallback if plugin not started
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    @Override
    public CompletableFuture<EmbeddingResult> embed(String text) {
        cancelled.set(false);

        if (!isConfigured()) {
            return CompletableFuture.failedFuture(
                    new EmbeddingProviderException("Ollama is not available or embedding model not installed. " //$NON-NLS-1$
                            + "Run 'ollama pull " + getModel() + "' to install.")); //$NON-NLS-1$ //$NON-NLS-2$
        }

        String requestBody = buildRequestBody(text);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getApiUrl() + "/api/embeddings")) //$NON-NLS-1$
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json") //$NON-NLS-1$ //$NON-NLS-2$
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        return getHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(this::parseResponse);
    }

    @Override
    public CompletableFuture<List<EmbeddingResult>> embedBatch(List<String> texts) {
        cancelled.set(false);

        if (!isConfigured()) {
            return CompletableFuture.failedFuture(
                    new EmbeddingProviderException("Ollama is not available or embedding model not installed")); //$NON-NLS-1$
        }

        if (texts.isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
        }

        // Ollama doesn't support batch embedding, process sequentially
        List<CompletableFuture<EmbeddingResult>> futures = new ArrayList<>();
        for (int i = 0; i < texts.size(); i++) {
            final int index = i;
            CompletableFuture<EmbeddingResult> future = embed(texts.get(i))
                    .thenApply(result -> new EmbeddingResult(result.getEmbedding(), index, result.getTokensUsed()));
            futures.add(future);
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    List<EmbeddingResult> results = new ArrayList<>();
                    for (CompletableFuture<EmbeddingResult> f : futures) {
                        results.add(f.join());
                    }
                    return results;
                });
    }

    private String buildRequestBody(String text) {
        JsonObject body = new JsonObject();
        body.addProperty("model", getModel()); //$NON-NLS-1$
        body.addProperty("prompt", text); //$NON-NLS-1$
        return gson.toJson(body);
    }

    private EmbeddingResult parseResponse(HttpResponse<String> response) {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw parseError(response);
        }

        try {
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();

            if (!json.has("embedding")) { //$NON-NLS-1$
                throw new EmbeddingProviderException("No embedding in Ollama response"); //$NON-NLS-1$
            }

            JsonArray embeddingArray = json.getAsJsonArray("embedding"); //$NON-NLS-1$
            float[] embedding = new float[embeddingArray.size()];
            for (int i = 0; i < embeddingArray.size(); i++) {
                embedding[i] = embeddingArray.get(i).getAsFloat();
            }

            // Ollama doesn't report token usage in embedding response
            return new EmbeddingResult(embedding, 0, 0);

        } catch (EmbeddingProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new EmbeddingProviderException("Failed to parse Ollama embedding response", e); //$NON-NLS-1$
        }
    }

    private EmbeddingProviderException parseError(HttpResponse<String> response) {
        String message = "Ollama embedding API error: " + response.statusCode(); //$NON-NLS-1$
        try {
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            if (json.has("error")) { //$NON-NLS-1$
                message = json.get("error").getAsString(); //$NON-NLS-1$
            }
        } catch (Exception ignored) {
            // Use default message
        }
        return new EmbeddingProviderException(message, null, response.statusCode(), null);
    }

    @Override
    public void cancel() {
        cancelled.set(true);
    }

    @Override
    public void dispose() {
        cancel();
        availabilityCache = null;
    }
}
