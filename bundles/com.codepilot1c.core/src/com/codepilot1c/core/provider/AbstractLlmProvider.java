/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
 */
package com.codepilot1c.core.provider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;

import com.codepilot1c.core.http.HttpClientConfig;
import com.codepilot1c.core.http.HttpClientFactory;
import com.codepilot1c.core.http.HttpRequestBodies;
import com.codepilot1c.core.internal.VibeCorePlugin;
import com.codepilot1c.core.settings.VibePreferenceConstants;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Abstract base class for LLM providers.
 */
public abstract class AbstractLlmProvider implements ILlmProvider {

    protected final Gson gson = new GsonBuilder().create();
    protected final AtomicBoolean cancelled = new AtomicBoolean(false);

    /**
     * Returns the preferences node for this plugin.
     *
     * @return the preferences
     */
    protected IEclipsePreferences getPreferences() {
        return InstanceScope.INSTANCE.getNode(VibeCorePlugin.PLUGIN_ID);
    }

    /**
     * Returns the configured request timeout in seconds.
     *
     * @return the timeout
     */
    protected int getRequestTimeout() {
        return getPreferences().getInt(VibePreferenceConstants.PREF_REQUEST_TIMEOUT, 60);
    }

    /**
     * Returns the shared HTTP client from the factory.
     *
     * <p>Uses the centralized {@link HttpClientFactory} which provides:
     * <ul>
     *   <li>HTTP/2 support with automatic fallback</li>
     *   <li>System proxy support</li>
     *   <li>Configurable redirects</li>
     * </ul>
     *
     * @return the HTTP client
     */
    protected HttpClient getHttpClient() {
        VibeCorePlugin plugin = VibeCorePlugin.getDefault();
        if (plugin != null) {
            HttpClientFactory factory = plugin.getHttpClientFactory();
            if (factory != null) {
                return factory.getSharedClient();
            }
        }
        // Fallback if plugin not started (e.g., testing)
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    /**
     * Returns the HTTP client configuration.
     *
     * @return the config, or null if not available
     */
    protected HttpClientConfig getHttpClientConfig() {
        VibeCorePlugin plugin = VibeCorePlugin.getDefault();
        if (plugin != null) {
            HttpClientFactory factory = plugin.getHttpClientFactory();
            return factory != null ? factory.getConfig() : null;
        }
        return null;
    }

    /**
     * Creates a POST request builder for the given URL.
     *
     * @param url the request URL
     * @return a request builder
     */
    protected HttpRequest.Builder createPostRequest(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(getRequestTimeout()))
                .header("Content-Type", "application/json"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * Creates a POST request with JSON body and optional GZIP compression.
     *
     * <p>Uses {@link HttpRequestBodies#postJson} to apply GZIP compression
     * when the request body exceeds the configured threshold.</p>
     *
     * @param url the request URL
     * @param json the JSON body
     * @return the configured request builder
     */
    protected HttpRequest.Builder createPostRequestWithBody(String url, String json) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(getRequestTimeout()));

        HttpClientConfig config = getHttpClientConfig();
        if (config != null) {
            return HttpRequestBodies.postJson(builder, json, config);
        }
        // Fallback without GZIP
        return HttpRequestBodies.postJsonUncompressed(builder, json);
    }

    /**
     * Sends an HTTP request asynchronously.
     *
     * @param request the request to send
     * @return a future with the response
     */
    protected CompletableFuture<HttpResponse<String>> sendAsync(HttpRequest request) {
        return getHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Sends an HTTP request asynchronously with streaming response.
     * Lines are processed as they arrive without blocking.
     *
     * @param request the HTTP request
     * @param lineProcessor processor for each SSE line; receives line and completion callback
     * @param errorHandler handler for errors
     * @return a future that completes when streaming is done
     */
    protected CompletableFuture<Void> sendAsyncStreaming(
            HttpRequest request,
            BiConsumer<String, Runnable> lineProcessor,
            java.util.function.Consumer<Throwable> errorHandler) {

        return getHttpClient()
                .sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                .thenAcceptAsync(response -> {
                    if (!isSuccess(response.statusCode())) {
                        // Read error body
                        try (InputStream is = response.body()) {
                            String errorBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                            errorHandler.accept(new LlmProviderException(
                                    "HTTP error: " + response.statusCode() + " - " + errorBody, //$NON-NLS-1$ //$NON-NLS-2$
                                    null, response.statusCode(), null));
                        } catch (IOException e) {
                            errorHandler.accept(new LlmProviderException(
                                    "HTTP error: " + response.statusCode(), e, response.statusCode(), null)); //$NON-NLS-1$
                        }
                        return;
                    }

                    // Process streaming response
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                        String line;
                        final boolean[] completed = { false };
                        Runnable completeCallback = () -> completed[0] = true;

                        while (!completed[0] && !isCancelled() && (line = reader.readLine()) != null) {
                            lineProcessor.accept(line, completeCallback);
                        }
                    } catch (IOException e) {
                        if (!isCancelled()) {
                            errorHandler.accept(new LlmProviderException(
                                    "Failed to read stream response", e)); //$NON-NLS-1$
                        }
                    }
                })
                .exceptionally(ex -> {
                    if (!isCancelled()) {
                        errorHandler.accept(ex);
                    }
                    return null;
                });
    }

    /**
     * Checks if a response indicates success.
     *
     * @param statusCode the HTTP status code
     * @return true if successful
     */
    protected boolean isSuccess(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }

    @Override
    public void cancel() {
        cancelled.set(true);
    }

    /**
     * Resets the cancelled state.
     */
    protected void resetCancelled() {
        cancelled.set(false);
    }

    /**
     * Checks if the operation has been cancelled.
     *
     * @return true if cancelled
     */
    protected boolean isCancelled() {
        return cancelled.get();
    }

    @Override
    public void dispose() {
        cancel();
        // HTTP client is managed by HttpClientFactory, no cleanup needed here
    }
}
