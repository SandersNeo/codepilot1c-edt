/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
 */
package com.codepilot1c.core.embedding;

/**
 * Exception thrown when an embedding operation fails.
 */
public class EmbeddingProviderException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int statusCode;
    private final String errorType;

    /**
     * Creates a new exception with a message.
     *
     * @param message the error message
     */
    public EmbeddingProviderException(String message) {
        this(message, null, 0, null);
    }

    /**
     * Creates a new exception with a message and cause.
     *
     * @param message the error message
     * @param cause the underlying cause
     */
    public EmbeddingProviderException(String message, Throwable cause) {
        this(message, cause, 0, null);
    }

    /**
     * Creates a new exception with full details.
     *
     * @param message the error message
     * @param cause the underlying cause
     * @param statusCode the HTTP status code (if applicable)
     * @param errorType the error type from the API (if applicable)
     */
    public EmbeddingProviderException(String message, Throwable cause, int statusCode, String errorType) {
        super(message, cause);
        this.statusCode = statusCode;
        this.errorType = errorType;
    }

    /**
     * Returns the HTTP status code.
     *
     * @return the status code, or 0 if not applicable
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Returns the error type from the API.
     *
     * @return the error type, or null if not applicable
     */
    public String getErrorType() {
        return errorType;
    }

    /**
     * Returns whether this is a rate limit error.
     *
     * @return true if rate limited
     */
    public boolean isRateLimitError() {
        return statusCode == 429 || "rate_limit_exceeded".equals(errorType); //$NON-NLS-1$
    }

    /**
     * Returns whether this is an authentication error.
     *
     * @return true if authentication failed
     */
    public boolean isAuthError() {
        return statusCode == 401 || statusCode == 403;
    }
}
