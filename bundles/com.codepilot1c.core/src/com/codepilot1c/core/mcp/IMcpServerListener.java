/*
 * Copyright (c) 2024 Example
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.codepilot1c.core.mcp;

import com.codepilot1c.core.mcp.config.McpServerConfig;
import com.codepilot1c.core.mcp.model.McpServerState;

/**
 * Listener for MCP server events.
 */
public interface IMcpServerListener {

    /**
     * Called when a server's state changes.
     *
     * @param config the server configuration
     * @param state the new state
     */
    void onServerStateChanged(McpServerConfig config, McpServerState state);

    /**
     * Called when a server is stopped.
     *
     * @param serverId the server ID
     */
    void onServerStopped(String serverId);
}
