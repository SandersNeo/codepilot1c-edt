/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
 */
package com.codepilot1c.core.mcp.model;

/**
 * Represents the lifecycle state of an MCP server.
 */
public enum McpServerState {
    /** Server is not running */
    STOPPED,
    /** Server is starting (connection in progress) */
    STARTING,
    /** Server is connected and operational */
    RUNNING,
    /** Server failed to start or crashed */
    ERROR
}
