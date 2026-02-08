/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
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
