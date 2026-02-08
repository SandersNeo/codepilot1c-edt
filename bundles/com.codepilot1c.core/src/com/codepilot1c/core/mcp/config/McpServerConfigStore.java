/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
 */
package com.codepilot1c.core.mcp.config;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.BackingStoreException;

import com.codepilot1c.core.logging.VibeLogger;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

/**
 * Persists MCP server configurations to Eclipse preferences.
 */
public class McpServerConfigStore {

    private static final VibeLogger.CategoryLogger LOG = VibeLogger.forClass(McpServerConfigStore.class);
    private static final String PREF_MCP_SERVERS = "mcp.servers";
    private static final String CORE_PLUGIN_ID = "com.codepilot1c.core";

    private static McpServerConfigStore instance;
    private List<McpServerConfig> servers;

    /**
     * Returns the singleton instance.
     *
     * @return the instance
     */
    public static synchronized McpServerConfigStore getInstance() {
        if (instance == null) {
            instance = new McpServerConfigStore();
        }
        return instance;
    }

    private McpServerConfigStore() {
    }

    /**
     * Returns all configured servers.
     *
     * @return list of servers (copy)
     */
    public List<McpServerConfig> getServers() {
        if (servers == null) {
            loadServers();
        }
        return new ArrayList<>(servers);
    }

    /**
     * Returns only enabled servers.
     *
     * @return list of enabled servers
     */
    public List<McpServerConfig> getEnabledServers() {
        return getServers().stream()
            .filter(McpServerConfig::isEnabled)
            .collect(Collectors.toList());
    }

    /**
     * Gets a server by ID.
     *
     * @param id the server ID
     * @return the server, or null if not found
     */
    public McpServerConfig getServer(String id) {
        return getServers().stream()
            .filter(s -> s.getId().equals(id))
            .findFirst()
            .orElse(null);
    }

    /**
     * Adds a server configuration.
     *
     * @param server the server to add
     */
    public void addServer(McpServerConfig server) {
        if (servers == null) {
            loadServers();
        }
        servers.add(server);
        saveServers();
    }

    /**
     * Updates a server configuration.
     *
     * @param server the server to update
     */
    public void updateServer(McpServerConfig server) {
        if (servers == null) {
            loadServers();
        }
        servers.removeIf(s -> s.getId().equals(server.getId()));
        servers.add(server);
        saveServers();
    }

    /**
     * Removes a server configuration.
     *
     * @param id the server ID
     */
    public void removeServer(String id) {
        if (servers == null) {
            loadServers();
        }
        servers.removeIf(s -> s.getId().equals(id));
        saveServers();
    }

    /**
     * Replaces all servers with the given list.
     *
     * @param newServers the new server list
     */
    public void setServers(List<McpServerConfig> newServers) {
        this.servers = new ArrayList<>(newServers);
        saveServers();
    }

    private void loadServers() {
        try {
            IPreferencesService prefsService = Platform.getPreferencesService();
            String json = prefsService.getString(CORE_PLUGIN_ID, PREF_MCP_SERVERS, "[]", null);
            servers = parseServersJson(json);
            LOG.debug("Loaded %d MCP server configurations", servers.size());
        } catch (Exception e) {
            LOG.error("Failed to load MCP server configs", e);
            servers = new ArrayList<>();
        }
    }

    private void saveServers() {
        try {
            IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(CORE_PLUGIN_ID);
            prefs.put(PREF_MCP_SERVERS, serversToJson(servers));
            prefs.flush();
            LOG.debug("Saved %d MCP server configurations", servers.size());
        } catch (BackingStoreException e) {
            LOG.error("Failed to save MCP server configs", e);
        }
    }

    private List<McpServerConfig> parseServersJson(String json) {
        List<McpServerConfig> result = new ArrayList<>();
        try {
            JsonElement element = JsonParser.parseString(json);
            if (element.isJsonArray()) {
                JsonArray array = element.getAsJsonArray();
                for (JsonElement elem : array) {
                    if (elem.isJsonObject()) {
                        try {
                            result.add(McpServerConfig.fromJson(elem.getAsJsonObject()));
                        } catch (Exception e) {
                            LOG.warn("Failed to parse MCP server config: %s", e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.warn("Failed to parse MCP servers JSON: %s", e.getMessage());
        }
        return result;
    }

    private String serversToJson(List<McpServerConfig> servers) {
        JsonArray array = new JsonArray();
        for (McpServerConfig config : servers) {
            array.add(config.toJson());
        }
        return array.toString();
    }
}
