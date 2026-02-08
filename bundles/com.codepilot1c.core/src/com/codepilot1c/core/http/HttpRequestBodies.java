/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
 */
package com.codepilot1c.core.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;

import com.codepilot1c.core.internal.VibeCorePlugin;

/**
 * Utility class for creating HTTP request bodies with optional GZIP compression.
 *
 * <p>Based on patterns from 1C:Workmate ({@code docs/workmate_decompiled/CodeAssistant.java}):
 * <ul>
 *   <li>Compress request body if size exceeds threshold</li>
 *   <li>Set {@code Content-Encoding: gzip} header when compressed</li>
 *   <li>Always set {@code Content-Type: application/json}</li>
 * </ul>
 */
public final class HttpRequestBodies {

    private static final String HEADER_CONTENT_TYPE = "Content-Type"; //$NON-NLS-1$
    private static final String HEADER_CONTENT_ENCODING = "Content-Encoding"; //$NON-NLS-1$
    private static final String CONTENT_TYPE_JSON = "application/json"; //$NON-NLS-1$
    private static final String ENCODING_GZIP = "gzip"; //$NON-NLS-1$

    private HttpRequestBodies() {
        // Utility class
    }

    /**
     * Sets a JSON POST body on the request builder with optional GZIP compression.
     *
     * <p>Compression is applied when:
     * <ul>
     *   <li>{@code config.isGzipRequestEnabled()} is true</li>
     *   <li>JSON size exceeds {@code config.getGzipMinBytes()}</li>
     * </ul>
     *
     * @param builder the request builder
     * @param json the JSON body
     * @param config the HTTP configuration
     * @return the updated request builder
     */
    public static HttpRequest.Builder postJson(
            HttpRequest.Builder builder,
            String json,
            HttpClientConfig config) {

        byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);

        // Always set Content-Type
        builder.header(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON);

        // Decide whether to compress
        if (config.isGzipRequestEnabled() && jsonBytes.length >= config.getGzipMinBytes()) {
            try {
                byte[] gzippedBytes = gzip(jsonBytes);

                // Only use gzip if it actually reduces size
                if (gzippedBytes.length < jsonBytes.length) {
                    builder.header(HEADER_CONTENT_ENCODING, ENCODING_GZIP);
                    builder.POST(HttpRequest.BodyPublishers.ofByteArray(gzippedBytes));
                    VibeCorePlugin.logInfo(String.format(
                            "GZIP compressed request: %d -> %d bytes (%.1f%%)", //$NON-NLS-1$
                            jsonBytes.length, gzippedBytes.length,
                            100.0 * gzippedBytes.length / jsonBytes.length));
                    return builder;
                }
            } catch (IOException e) {
                VibeCorePlugin.logWarn("Failed to GZIP compress request, sending uncompressed", e); //$NON-NLS-1$
            }
        }

        // Send uncompressed
        builder.POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));
        return builder;
    }

    /**
     * Creates a JSON POST body without compression.
     *
     * @param builder the request builder
     * @param json the JSON body
     * @return the updated request builder
     */
    public static HttpRequest.Builder postJsonUncompressed(
            HttpRequest.Builder builder,
            String json) {

        return builder
                .header(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));
    }

    /**
     * Compresses data using GZIP.
     *
     * @param data the data to compress
     * @return the compressed data
     * @throws IOException if compression fails
     */
    private static byte[] gzip(byte[] data) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length);
        try (GZIPOutputStream gzip = new GZIPOutputStream(bos)) {
            gzip.write(data);
        }
        return bos.toByteArray();
    }

    /**
     * Checks if a request body should be compressed based on config and size.
     *
     * @param bodySize the size in bytes
     * @param config the HTTP configuration
     * @return true if compression should be applied
     */
    public static boolean shouldCompress(int bodySize, HttpClientConfig config) {
        return config.isGzipRequestEnabled() && bodySize >= config.getGzipMinBytes();
    }
}
