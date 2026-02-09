/*
 * Copyright (c) 2024 Example
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 * SPDX-License-Identifier: AGPL-3.0-only
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
