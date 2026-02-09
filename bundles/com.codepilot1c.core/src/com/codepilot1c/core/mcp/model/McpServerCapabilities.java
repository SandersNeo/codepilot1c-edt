/*
 * Copyright (c) 2024 Example
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.codepilot1c.core.mcp.model;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Represents the capabilities of an MCP server.
 *
 * <p>Capabilities are returned during the initialize handshake and
 * indicate what features the server supports.</p>
 */
public class McpServerCapabilities {

    private boolean supportsTools;
    private boolean supportsResources;
    private boolean supportsPrompts;
    private boolean supportsLogging;
    private String serverName;
    private String serverVersion;

    /**
     * Creates empty capabilities.
     */
    public McpServerCapabilities() {
    }

    /**
     * Parses capabilities from an initialize response.
     *
     * @param result the result object from initialize
     * @return the parsed capabilities
     */
    public static McpServerCapabilities fromInitializeResult(Object result) {
        McpServerCapabilities caps = new McpServerCapabilities();

        if (result == null) {
            return caps;
        }

        // Convert result to JsonObject if needed (Gson may return LinkedTreeMap)
        JsonObject json = null;
        if (result instanceof JsonObject) {
            json = (JsonObject) result;
        } else {
            // Convert LinkedTreeMap or other types to JsonObject
            Gson gson = new Gson();
            JsonElement element = gson.toJsonTree(result);
            if (element.isJsonObject()) {
                json = element.getAsJsonObject();
            }
        }

        if (json == null) {
            return caps;
        }

        // Parse capabilities
        if (json.has("capabilities")) {
            JsonObject capsObj = json.getAsJsonObject("capabilities");
            caps.supportsTools = capsObj.has("tools");
            caps.supportsResources = capsObj.has("resources");
            caps.supportsPrompts = capsObj.has("prompts");
            caps.supportsLogging = capsObj.has("logging");
        }

        // Parse server info
        if (json.has("serverInfo")) {
            JsonObject serverInfo = json.getAsJsonObject("serverInfo");
            if (serverInfo.has("name")) {
                caps.serverName = serverInfo.get("name").getAsString();
            }
            if (serverInfo.has("version")) {
                caps.serverVersion = serverInfo.get("version").getAsString();
            }
        }

        return caps;
    }

    /**
     * Returns whether the server supports tools.
     *
     * @return true if tools are supported
     */
    public boolean supportsTools() {
        return supportsTools;
    }

    /**
     * Sets whether the server supports tools.
     *
     * @param supportsTools true if tools are supported
     */
    public void setSupportsTools(boolean supportsTools) {
        this.supportsTools = supportsTools;
    }

    /**
     * Returns whether the server supports resources.
     *
     * @return true if resources are supported
     */
    public boolean supportsResources() {
        return supportsResources;
    }

    /**
     * Sets whether the server supports resources.
     *
     * @param supportsResources true if resources are supported
     */
    public void setSupportsResources(boolean supportsResources) {
        this.supportsResources = supportsResources;
    }

    /**
     * Returns whether the server supports prompts.
     *
     * @return true if prompts are supported
     */
    public boolean supportsPrompts() {
        return supportsPrompts;
    }

    /**
     * Sets whether the server supports prompts.
     *
     * @param supportsPrompts true if prompts are supported
     */
    public void setSupportsPrompts(boolean supportsPrompts) {
        this.supportsPrompts = supportsPrompts;
    }

    /**
     * Returns whether the server supports logging.
     *
     * @return true if logging is supported
     */
    public boolean supportsLogging() {
        return supportsLogging;
    }

    /**
     * Sets whether the server supports logging.
     *
     * @param supportsLogging true if logging is supported
     */
    public void setSupportsLogging(boolean supportsLogging) {
        this.supportsLogging = supportsLogging;
    }

    /**
     * Returns the server name.
     *
     * @return the server name
     */
    public String getServerName() {
        return serverName;
    }

    /**
     * Sets the server name.
     *
     * @param serverName the server name
     */
    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    /**
     * Returns the server version.
     *
     * @return the server version
     */
    public String getServerVersion() {
        return serverVersion;
    }

    /**
     * Sets the server version.
     *
     * @param serverVersion the server version
     */
    public void setServerVersion(String serverVersion) {
        this.serverVersion = serverVersion;
    }

    @Override
    public String toString() {
        return "McpServerCapabilities[tools=" + supportsTools +
               ", resources=" + supportsResources +
               ", prompts=" + supportsPrompts +
               ", server=" + serverName + " " + serverVersion + "]";
    }
}
