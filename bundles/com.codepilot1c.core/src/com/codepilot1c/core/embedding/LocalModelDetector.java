/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
 */
package com.codepilot1c.core.embedding;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;

import com.codepilot1c.core.embedding.ollama.OllamaEmbeddingProvider;
import com.codepilot1c.core.internal.VibeCorePlugin;
import com.codepilot1c.core.settings.VibePreferenceConstants;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Detects locally available embedding models for zero-config operation.
 *
 * <p>This class enables automatic embedding provider selection without requiring
 * user configuration. It follows a priority order:</p>
 * <ol>
 *   <li>Local Ollama with embedding model (nomic-embed-text)</li>
 *   <li>OpenAI (if API key is configured)</li>
 *   <li>Disabled (no provider available)</li>
 * </ol>
 *
 * <p>This mirrors the approach used by Cursor and GitHub Copilot where
 * embeddings work transparently without configuration.</p>
 */
public final class LocalModelDetector {

    /** Ollama API endpoint for listing models. */
    private static final String OLLAMA_TAGS_ENDPOINT = "/api/tags"; //$NON-NLS-1$

    /** Recommended embedding models in priority order. */
    private static final String[] RECOMMENDED_EMBEDDING_MODELS = {
            "nomic-embed-text", //$NON-NLS-1$
            "mxbai-embed-large", //$NON-NLS-1$
            "all-minilm", //$NON-NLS-1$
            "bge-small", //$NON-NLS-1$
            "bge-large" //$NON-NLS-1$
    };

    private final HttpClient httpClient;

    /**
     * Detection result containing provider information.
     */
    public static class DetectionResult {
        private final String providerId;
        private final String modelName;
        private final boolean available;
        private final String message;

        private DetectionResult(String providerId, String modelName, boolean available, String message) {
            this.providerId = providerId;
            this.modelName = modelName;
            this.available = available;
            this.message = message;
        }

        /** Returns the detected provider ID (e.g., "ollama", "openai"). */
        public String getProviderId() {
            return providerId;
        }

        /** Returns the detected model name, or null if not applicable. */
        public String getModelName() {
            return modelName;
        }

        /** Returns true if a usable provider was detected. */
        public boolean isAvailable() {
            return available;
        }

        /** Returns a human-readable status message. */
        public String getMessage() {
            return message;
        }

        /** Creates a result for Ollama provider. */
        public static DetectionResult ollama(String model) {
            return new DetectionResult("ollama", model, true, //$NON-NLS-1$
                    "Using local Ollama with " + model); //$NON-NLS-1$
        }

        /** Creates a result for OpenAI provider. */
        public static DetectionResult openai() {
            return new DetectionResult("openai", "text-embedding-3-small", true, //$NON-NLS-1$ //$NON-NLS-2$
                    "Using OpenAI embeddings"); //$NON-NLS-1$
        }

        /** Creates a result when no provider is available. */
        public static DetectionResult none(String reason) {
            return new DetectionResult(null, null, false, reason);
        }
    }

    /**
     * Creates a new detector with a default HTTP client.
     */
    public LocalModelDetector() {
        this(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build());
    }

    /**
     * Creates a new detector with the specified HTTP client.
     *
     * @param httpClient the HTTP client to use
     */
    public LocalModelDetector(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * Detects the best available embedding provider.
     *
     * <p>Checks in order: Ollama (local), OpenAI (if configured).
     * Returns immediately when a suitable provider is found.</p>
     *
     * @return the detection result
     */
    public DetectionResult detect() {
        // First, check Ollama (local, no API key needed)
        DetectionResult ollamaResult = checkOllama();
        if (ollamaResult.isAvailable()) {
            return ollamaResult;
        }

        // Second, check if OpenAI is configured
        if (isOpenAiConfigured()) {
            return DetectionResult.openai();
        }

        // No provider available
        return DetectionResult.none(
                "No embedding provider available. Either install Ollama with 'ollama pull nomic-embed-text' " //$NON-NLS-1$
                        + "or configure an OpenAI API key."); //$NON-NLS-1$
    }

    /**
     * Detects available providers asynchronously.
     *
     * @return a future containing the detection result
     */
    public CompletableFuture<DetectionResult> detectAsync() {
        return CompletableFuture.supplyAsync(this::detect);
    }

    /**
     * Checks if Ollama is running and has an embedding model available.
     *
     * @return detection result for Ollama
     */
    private DetectionResult checkOllama() {
        String ollamaUrl = getOllamaUrl();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ollamaUrl + OLLAMA_TAGS_ENDPOINT))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return DetectionResult.none("Ollama not responding"); //$NON-NLS-1$
            }

            // Parse available models
            List<String> availableModels = parseOllamaModels(response.body());

            // Find the best embedding model
            String bestModel = findBestEmbeddingModel(availableModels);
            if (bestModel != null) {
                return DetectionResult.ollama(bestModel);
            }

            return DetectionResult.none("Ollama running but no embedding model found. " //$NON-NLS-1$
                    + "Run 'ollama pull nomic-embed-text' to install."); //$NON-NLS-1$

        } catch (Exception e) {
            return DetectionResult.none("Ollama not available: " + e.getMessage()); //$NON-NLS-1$
        }
    }

    /**
     * Parses the list of model names from Ollama /api/tags response.
     */
    private List<String> parseOllamaModels(String responseBody) {
        List<String> models = new ArrayList<>();
        try {
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            if (json.has("models")) { //$NON-NLS-1$
                JsonArray modelsArray = json.getAsJsonArray("models"); //$NON-NLS-1$
                for (int i = 0; i < modelsArray.size(); i++) {
                    JsonObject modelObj = modelsArray.get(i).getAsJsonObject();
                    String name = modelObj.get("name").getAsString(); //$NON-NLS-1$
                    // Remove tag suffix (e.g., "nomic-embed-text:latest" -> "nomic-embed-text")
                    int colonIdx = name.indexOf(':');
                    if (colonIdx > 0) {
                        name = name.substring(0, colonIdx);
                    }
                    models.add(name);
                }
            }
        } catch (Exception e) {
            VibeCorePlugin.logWarn("Failed to parse Ollama models response", e); //$NON-NLS-1$
        }
        return models;
    }

    /**
     * Finds the best embedding model from available models.
     */
    private String findBestEmbeddingModel(List<String> availableModels) {
        // Check recommended models in priority order
        for (String recommended : RECOMMENDED_EMBEDDING_MODELS) {
            for (String available : availableModels) {
                if (available.contains(recommended)) {
                    return available;
                }
            }
        }
        return null;
    }

    private static final String DEFAULT_OPENAI_URL = "https://api.openai.com/v1"; //$NON-NLS-1$

    /**
     * Checks if OpenAI embedding is configured.
     *
     * <p>A custom OpenAI-compatible endpoint is considered configured even without
     * an API key, as some deployments don't require authentication.</p>
     */
    private boolean isOpenAiConfigured() {
        IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(VibeCorePlugin.PLUGIN_ID);

        // Check if custom URL is configured (doesn't require API key)
        String embeddingUrl = prefs.get(VibePreferenceConstants.PREF_EMBEDDING_API_URL, ""); //$NON-NLS-1$
        if (!embeddingUrl.isEmpty() && !embeddingUrl.equals(DEFAULT_OPENAI_URL)) {
            return true;
        }

        // Check embedding-specific API key
        String embeddingKey = prefs.get(VibePreferenceConstants.PREF_EMBEDDING_API_KEY, ""); //$NON-NLS-1$
        if (!embeddingKey.isEmpty()) {
            return true;
        }

        // Fall back to main OpenAI API key
        String openaiKey = prefs.get(VibePreferenceConstants.PREF_OPENAI_API_KEY, ""); //$NON-NLS-1$
        return !openaiKey.isEmpty();
    }

    /**
     * Gets the Ollama API URL from preferences.
     */
    private String getOllamaUrl() {
        IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(VibeCorePlugin.PLUGIN_ID);
        String url = prefs.get(VibePreferenceConstants.PREF_OLLAMA_EMBEDDING_API_URL, ""); //$NON-NLS-1$
        if (url.isEmpty()) {
            url = prefs.get(VibePreferenceConstants.PREF_OLLAMA_API_URL, OllamaEmbeddingProvider.DEFAULT_API_URL);
        }
        // Remove trailing slash
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url; //$NON-NLS-1$
    }

    /**
     * Lists all available Ollama embedding models.
     *
     * @return list of model names, empty if Ollama not available
     */
    public List<String> listOllamaEmbeddingModels() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(getOllamaUrl() + OLLAMA_TAGS_ENDPOINT))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                List<String> allModels = parseOllamaModels(response.body());
                // Filter to known embedding models
                List<String> embeddingModels = new ArrayList<>();
                for (String model : allModels) {
                    for (String recommended : RECOMMENDED_EMBEDDING_MODELS) {
                        if (model.contains(recommended)) {
                            embeddingModels.add(model);
                            break;
                        }
                    }
                }
                return embeddingModels;
            }
        } catch (Exception e) {
            // Ollama not available
        }
        return List.of();
    }
}
