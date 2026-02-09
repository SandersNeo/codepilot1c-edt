/*
 * Copyright (c) 2024 Example
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.codepilot1c.core.tools;

/**
 * Result of executing a tool.
 */
public class ToolResult {

    private final boolean success;
    private final String content;
    private final String errorMessage;
    private final ToolResultType type;

    private ToolResult(boolean success, String content, String errorMessage, ToolResultType type) {
        this.success = success;
        this.content = content;
        this.errorMessage = errorMessage;
        this.type = type;
    }

    /**
     * Creates a successful text result.
     *
     * @param content the result content
     * @return the tool result
     */
    public static ToolResult success(String content) {
        return new ToolResult(true, content, null, ToolResultType.TEXT);
    }

    /**
     * Creates a successful result with specific type.
     *
     * @param content the result content
     * @param type the result type
     * @return the tool result
     */
    public static ToolResult success(String content, ToolResultType type) {
        return new ToolResult(true, content, null, type);
    }

    /**
     * Creates a failure result.
     *
     * @param errorMessage the error message
     * @return the tool result
     */
    public static ToolResult failure(String errorMessage) {
        return new ToolResult(false, null, errorMessage, ToolResultType.ERROR);
    }

    /**
     * Returns whether the tool execution was successful.
     *
     * @return true if successful
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Returns the result content.
     *
     * @return the content, or null if failed
     */
    public String getContent() {
        return content;
    }

    /**
     * Returns the error message if execution failed.
     *
     * @return the error message, or null if successful
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Returns the result type.
     *
     * @return the result type
     */
    public ToolResultType getType() {
        return type;
    }

    /**
     * Returns the content to send back to the LLM.
     *
     * @return the content or error message
     */
    public String getContentForLlm() {
        if (success) {
            return content;
        } else {
            return "Error: " + errorMessage; //$NON-NLS-1$
        }
    }

    /**
     * Type of tool result.
     */
    public enum ToolResultType {
        /** Plain text result */
        TEXT,
        /** Code/file content */
        CODE,
        /** Search results with multiple items */
        SEARCH_RESULTS,
        /** File list */
        FILE_LIST,
        /** Confirmation of action taken */
        CONFIRMATION,
        /** Error result */
        ERROR
    }
}
