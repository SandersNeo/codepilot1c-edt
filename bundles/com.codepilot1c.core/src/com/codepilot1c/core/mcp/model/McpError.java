/*
 * Copyright (c) 2024 Example
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.codepilot1c.core.mcp.model;

/**
 * Represents a JSON-RPC 2.0 error in MCP protocol.
 */
public class McpError {

    private int code;
    private String message;
    private Object data;

    /**
     * Creates a new MCP error.
     */
    public McpError() {
    }

    /**
     * Creates a new MCP error with all fields.
     *
     * @param code the error code
     * @param message the error message
     * @param data optional error data
     */
    public McpError(int code, String message, Object data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * Returns the error code.
     *
     * @return the error code (JSON-RPC standard codes or custom)
     */
    public int getCode() {
        return code;
    }

    /**
     * Sets the error code.
     *
     * @param code the error code
     */
    public void setCode(int code) {
        this.code = code;
    }

    /**
     * Returns the error message.
     *
     * @return the error message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the error message.
     *
     * @param message the error message
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Returns optional error data.
     *
     * @return the error data, may be null
     */
    public Object getData() {
        return data;
    }

    /**
     * Sets the error data.
     *
     * @param data the error data
     */
    public void setData(Object data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "McpError[code=" + code + ", message=" + message + "]";
    }
}
