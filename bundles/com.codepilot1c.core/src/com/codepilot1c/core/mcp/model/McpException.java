/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
 */
package com.codepilot1c.core.mcp.model;

/**
 * Exception thrown when an MCP operation fails.
 */
public class McpException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int errorCode;

    /**
     * Creates a new MCP exception from an MCP error.
     *
     * @param error the MCP error
     */
    public McpException(McpError error) {
        super(error.getMessage());
        this.errorCode = error.getCode();
    }

    /**
     * Creates a new MCP exception with a message.
     *
     * @param message the error message
     */
    public McpException(String message) {
        super(message);
        this.errorCode = -1;
    }

    /**
     * Creates a new MCP exception with a message and cause.
     *
     * @param message the error message
     * @param cause the cause
     */
    public McpException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = -1;
    }

    /**
     * Returns the MCP error code.
     *
     * @return the error code, or -1 if not from MCP error
     */
    public int getErrorCode() {
        return errorCode;
    }
}
