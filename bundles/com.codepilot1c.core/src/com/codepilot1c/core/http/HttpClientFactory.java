/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
 */
package com.codepilot1c.core.http;

import java.net.http.HttpClient;

/**
 * Factory for creating and managing shared HTTP client instances.
 *
 * <p>Provides centralized HTTP client configuration including:
 * <ul>
 *   <li>HTTP/2 support with automatic fallback to HTTP/1.1</li>
 *   <li>System proxy support</li>
 *   <li>Configurable redirects</li>
 *   <li>GZIP request compression</li>
 * </ul>
 *
 * <p>Implementation is based on patterns from 1C:Workmate
 * ({@code docs/workmate_decompiled/HttpClientBuilder.java}).</p>
 */
public interface HttpClientFactory {

    /**
     * Returns the shared HTTP client instance.
     *
     * <p>The client is configured according to the current settings
     * and is reused across all providers.</p>
     *
     * @return the shared HTTP client
     */
    HttpClient getSharedClient();

    /**
     * Returns the current HTTP configuration.
     *
     * @return the configuration
     */
    HttpClientConfig getConfig();

    /**
     * Refreshes the HTTP client with current settings.
     *
     * <p>Call this method after preference changes to rebuild
     * the client with updated configuration.</p>
     */
    void refresh();

    /**
     * Disposes the factory and releases resources.
     *
     * <p>Should be called on plugin stop to clean up executor threads.</p>
     */
    void dispose();
}
