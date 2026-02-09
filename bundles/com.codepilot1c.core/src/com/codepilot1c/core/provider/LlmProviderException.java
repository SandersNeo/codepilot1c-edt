/*
 * Copyright (c) 2024 Example
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.codepilot1c.core.provider;

/**
 * Exception thrown when an LLM provider encounters an error.
 */
public class LlmProviderException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int statusCode;
    private final String errorType;

    /**
     * Creates a new exception with a message.
     *
     * @param message the error message
     */
    public LlmProviderException(String message) {
        this(message, null, 0, null);
    }

    /**
     * Creates a new exception with a message and cause.
     *
     * @param message the error message
     * @param cause   the cause
     */
    public LlmProviderException(String message, Throwable cause) {
        this(message, cause, 0, null);
    }

    /**
     * Creates a new exception with full details.
     *
     * @param message    the error message
     * @param cause      the cause
     * @param statusCode the HTTP status code (if applicable)
     * @param errorType  the error type from the API
     */
    public LlmProviderException(String message, Throwable cause, int statusCode, String errorType) {
        super(message, cause);
        this.statusCode = statusCode;
        this.errorType = errorType;
    }

    /**
     * Returns the HTTP status code, or 0 if not applicable.
     *
     * @return the status code
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Returns the error type from the API, or null if not available.
     *
     * @return the error type
     */
    public String getErrorType() {
        return errorType;
    }

    /**
     * Checks if this is a rate limit error.
     *
     * @return true if rate limited
     */
    public boolean isRateLimitError() {
        return statusCode == 429 || "rate_limit_error".equals(errorType); //$NON-NLS-1$
    }

    /**
     * Checks if this is an authentication error.
     *
     * @return true if authentication failed
     */
    public boolean isAuthenticationError() {
        return statusCode == 401 || statusCode == 403 || "authentication_error".equals(errorType); //$NON-NLS-1$
    }
}
