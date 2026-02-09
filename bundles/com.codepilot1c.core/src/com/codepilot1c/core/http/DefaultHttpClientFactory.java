/*
 * Copyright (c) 2024 Example
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.codepilot1c.core.http;

import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;

import com.codepilot1c.core.internal.VibeCorePlugin;
import com.codepilot1c.core.settings.VibePreferenceConstants;

/**
 * Default implementation of {@link HttpClientFactory}.
 *
 * <p>Creates an HTTP client with the following features (matching Workmate patterns):
 * <ul>
 *   <li>HTTP/2 with automatic fallback to HTTP/1.1</li>
 *   <li>System proxy support via {@link ProxySelector#getDefault()}</li>
 *   <li>Normal redirect handling</li>
 *   <li>Configurable timeouts</li>
 *   <li>Dedicated executor for async operations</li>
 * </ul>
 *
 * @see HttpClientFactory
 */
public class DefaultHttpClientFactory implements HttpClientFactory {

    private volatile HttpClient httpClient;
    private volatile HttpClientConfig config;
    private volatile ExecutorService executor;

    /**
     * Creates a new factory with default configuration.
     */
    public DefaultHttpClientFactory() {
        this.config = loadConfigFromPreferences();
        this.executor = createExecutor();
        this.httpClient = buildClient();
    }

    @Override
    public HttpClient getSharedClient() {
        HttpClient client = httpClient;
        if (client == null) {
            synchronized (this) {
                client = httpClient;
                if (client == null) {
                    client = buildClient();
                    httpClient = client;
                }
            }
        }
        return client;
    }

    @Override
    public HttpClientConfig getConfig() {
        return config;
    }

    @Override
    public synchronized void refresh() {
        // Reload configuration from preferences
        this.config = loadConfigFromPreferences();

        // Rebuild the client
        this.httpClient = buildClient();

        VibeCorePlugin.logInfo("HTTP client refreshed with new configuration"); //$NON-NLS-1$
    }

    @Override
    public synchronized void dispose() {
        httpClient = null;

        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            executor = null;
        }

        VibeCorePlugin.logInfo("HTTP client factory disposed"); //$NON-NLS-1$
    }

    /**
     * Builds a new HTTP client based on current configuration.
     *
     * <p>Pattern based on {@code docs/workmate_decompiled/HttpClientBuilder.java}:
     * <pre>
     * HttpClient.newBuilder()
     *     .version(HttpClient.Version.HTTP_2)
     *     .followRedirects(HttpClient.Redirect.NORMAL)
     *     .proxy(ProxySelector.getDefault())
     * </pre>
     *
     * @return the configured HTTP client
     */
    private HttpClient buildClient() {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(config.getConnectTimeout())
                .followRedirects(config.getRedirectPolicy());

        // HTTP/2 support (with automatic fallback to HTTP/1.1)
        if (config.isHttp2Enabled()) {
            builder.version(HttpClient.Version.HTTP_2);
        } else {
            builder.version(HttpClient.Version.HTTP_1_1);
        }

        // System proxy support
        if (config.isUseSystemProxy()) {
            builder.proxy(ProxySelector.getDefault());
        }

        // Dedicated executor for async operations
        if (executor != null && !executor.isShutdown()) {
            builder.executor(executor);
        }

        return builder.build();
    }

    /**
     * Creates an executor for HTTP async operations.
     *
     * @return the executor service
     */
    private ExecutorService createExecutor() {
        return Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "Vibe-HTTP-Worker"); //$NON-NLS-1$
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Loads HTTP configuration from Eclipse preferences.
     *
     * @return the configuration
     */
    private HttpClientConfig loadConfigFromPreferences() {
        IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(VibeCorePlugin.PLUGIN_ID);

        int timeout = prefs.getInt(VibePreferenceConstants.PREF_REQUEST_TIMEOUT, 60);
        boolean http2Enabled = prefs.getBoolean(VibePreferenceConstants.PREF_HTTP_HTTP2_ENABLED, true);
        boolean useProxy = prefs.getBoolean(VibePreferenceConstants.PREF_HTTP_USE_SYSTEM_PROXY, true);
        boolean gzipEnabled = prefs.getBoolean(VibePreferenceConstants.PREF_HTTP_GZIP_ENABLED, true);
        int gzipMinBytes = prefs.getInt(VibePreferenceConstants.PREF_HTTP_GZIP_MIN_BYTES, 1024);

        return HttpClientConfig.builder()
                .connectTimeout(Duration.ofSeconds(30))
                .requestTimeout(Duration.ofSeconds(timeout))
                .http2Enabled(http2Enabled)
                .useSystemProxy(useProxy)
                .redirectPolicy(HttpClient.Redirect.NORMAL)
                .gzipRequestEnabled(gzipEnabled)
                .gzipMinBytes(gzipMinBytes)
                .build();
    }
}
