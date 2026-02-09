/*
 * Copyright (c) 2024 Example
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.codepilot1c.core.http;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Configuration for HTTP client.
 *
 * <p>Centralizes HTTP settings including timeouts, HTTP/2 support,
 * proxy configuration, and GZIP compression options.</p>
 */
public final class HttpClientConfig {

    private final Duration connectTimeout;
    private final Duration requestTimeout;
    private final boolean http2Enabled;
    private final boolean useSystemProxy;
    private final HttpClient.Redirect redirectPolicy;
    private final boolean gzipRequestEnabled;
    private final int gzipMinBytes;

    private HttpClientConfig(Builder builder) {
        this.connectTimeout = builder.connectTimeout;
        this.requestTimeout = builder.requestTimeout;
        this.http2Enabled = builder.http2Enabled;
        this.useSystemProxy = builder.useSystemProxy;
        this.redirectPolicy = builder.redirectPolicy;
        this.gzipRequestEnabled = builder.gzipRequestEnabled;
        this.gzipMinBytes = builder.gzipMinBytes;
    }

    /**
     * Returns the connection timeout.
     *
     * @return the connect timeout
     */
    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * Returns the default request timeout.
     *
     * @return the request timeout
     */
    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    /**
     * Returns whether HTTP/2 is enabled.
     *
     * @return true if HTTP/2 is enabled
     */
    public boolean isHttp2Enabled() {
        return http2Enabled;
    }

    /**
     * Returns whether system proxy should be used.
     *
     * @return true if system proxy is enabled
     */
    public boolean isUseSystemProxy() {
        return useSystemProxy;
    }

    /**
     * Returns the redirect policy.
     *
     * @return the redirect policy
     */
    public HttpClient.Redirect getRedirectPolicy() {
        return redirectPolicy;
    }

    /**
     * Returns whether GZIP request compression is enabled.
     *
     * @return true if GZIP is enabled
     */
    public boolean isGzipRequestEnabled() {
        return gzipRequestEnabled;
    }

    /**
     * Returns the minimum request body size in bytes for GZIP compression.
     *
     * @return the minimum size for GZIP
     */
    public int getGzipMinBytes() {
        return gzipMinBytes;
    }

    /**
     * Creates a new builder with default values.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for HttpClientConfig.
     */
    public static final class Builder {
        private Duration connectTimeout = Duration.ofSeconds(30);
        private Duration requestTimeout = Duration.ofSeconds(60);
        private boolean http2Enabled = true;
        private boolean useSystemProxy = true;
        private HttpClient.Redirect redirectPolicy = HttpClient.Redirect.NORMAL;
        private boolean gzipRequestEnabled = true;
        private int gzipMinBytes = 1024;

        private Builder() {
        }

        /**
         * Sets the connection timeout.
         *
         * @param connectTimeout the timeout
         * @return this builder
         */
        public Builder connectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        /**
         * Sets the default request timeout.
         *
         * @param requestTimeout the timeout
         * @return this builder
         */
        public Builder requestTimeout(Duration requestTimeout) {
            this.requestTimeout = requestTimeout;
            return this;
        }

        /**
         * Sets whether HTTP/2 is enabled.
         *
         * @param http2Enabled true to enable HTTP/2
         * @return this builder
         */
        public Builder http2Enabled(boolean http2Enabled) {
            this.http2Enabled = http2Enabled;
            return this;
        }

        /**
         * Sets whether to use system proxy.
         *
         * @param useSystemProxy true to use system proxy
         * @return this builder
         */
        public Builder useSystemProxy(boolean useSystemProxy) {
            this.useSystemProxy = useSystemProxy;
            return this;
        }

        /**
         * Sets the redirect policy.
         *
         * @param redirectPolicy the policy
         * @return this builder
         */
        public Builder redirectPolicy(HttpClient.Redirect redirectPolicy) {
            this.redirectPolicy = redirectPolicy;
            return this;
        }

        /**
         * Sets whether GZIP request compression is enabled.
         *
         * @param gzipRequestEnabled true to enable
         * @return this builder
         */
        public Builder gzipRequestEnabled(boolean gzipRequestEnabled) {
            this.gzipRequestEnabled = gzipRequestEnabled;
            return this;
        }

        /**
         * Sets the minimum size for GZIP compression.
         *
         * @param gzipMinBytes minimum bytes
         * @return this builder
         */
        public Builder gzipMinBytes(int gzipMinBytes) {
            this.gzipMinBytes = gzipMinBytes;
            return this;
        }

        /**
         * Builds the configuration.
         *
         * @return the config
         */
        public HttpClientConfig build() {
            return new HttpClientConfig(this);
        }
    }
}
