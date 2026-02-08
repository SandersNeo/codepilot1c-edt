/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
 */
package com.codepilot1c.core.mcp;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.codepilot1c.core.mcp.client.McpClient;
import com.codepilot1c.core.mcp.model.McpContent;
import com.codepilot1c.core.mcp.model.McpTool;
import com.codepilot1c.core.mcp.model.McpToolResult;
import com.codepilot1c.core.tools.ITool;
import com.codepilot1c.core.tools.ToolResult;

/**
 * Adapts an MCP tool to the ITool interface.
 *
 * <p>This allows MCP tools to be used seamlessly with the existing tool system.</p>
 */
public class McpToolAdapter implements ITool {

    private final McpClient client;
    private final McpTool mcpTool;
    private final String serverName;

    /**
     * Creates a new adapter.
     *
     * @param client the MCP client
     * @param mcpTool the MCP tool definition
     */
    public McpToolAdapter(McpClient client, McpTool mcpTool) {
        this.client = client;
        this.mcpTool = mcpTool;
        this.serverName = client.getServerName();
    }

    @Override
    public String getName() {
        // Prefix with server name to avoid collisions
        // Sanitize server name for valid tool name
        String sanitizedName = serverName.replaceAll("[^a-zA-Z0-9_]", "_").toLowerCase();
        return "mcp_" + sanitizedName + "_" + mcpTool.getName();
    }

    @Override
    public String getDescription() {
        return "[MCP:" + serverName + "] " + mcpTool.getDescription();
    }

    @Override
    public String getParameterSchema() {
        if (mcpTool.getInputSchema() != null) {
            return mcpTool.getInputSchema().toString();
        }
        return "{\"type\":\"object\",\"properties\":{}}";
    }

    @Override
    public CompletableFuture<ToolResult> execute(Map<String, Object> params) {
        return client.callTool(mcpTool.getName(), params)
            .thenApply(this::convertResult)
            .exceptionally(e -> {
                String errorMsg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
                return ToolResult.failure("MCP tool error: " + errorMsg);
            });
    }

    private ToolResult convertResult(McpToolResult mcpResult) {
        if (mcpResult.isError()) {
            return ToolResult.failure(extractErrorText(mcpResult));
        }

        StringBuilder text = new StringBuilder();
        for (McpContent content : mcpResult.getContent()) {
            if (content.getType() == McpContent.Type.TEXT) {
                if (text.length() > 0) {
                    text.append("\n");
                }
                text.append(content.getText());
            }
            // TODO v2: Handle images, resources
        }

        return ToolResult.success(text.toString().trim());
    }

    private String extractErrorText(McpToolResult mcpResult) {
        for (McpContent content : mcpResult.getContent()) {
            if (content.getType() == McpContent.Type.TEXT && content.getText() != null) {
                return content.getText();
            }
        }
        return "Unknown MCP error";
    }

    @Override
    public boolean requiresConfirmation() {
        return false;
    }

    @Override
    public boolean isDestructive() {
        // Heuristic based on tool name
        String name = mcpTool.getName().toLowerCase();
        return name.contains("delete") || name.contains("remove") ||
               name.contains("write") || name.contains("create") ||
               name.contains("update") || name.contains("modify");
    }

    /**
     * Returns the original MCP tool.
     *
     * @return the MCP tool
     */
    public McpTool getMcpTool() {
        return mcpTool;
    }

    /**
     * Returns the server name.
     *
     * @return the server name
     */
    public String getServerName() {
        return serverName;
    }
}
