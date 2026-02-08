/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
 */
package com.codepilot1c.core.mcp.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Configuration for an MCP server.
 */
public class McpServerConfig {

    private String id;
    private String name;
    private boolean enabled;
    private TransportType transportType;

    // STDIO config
    private String command;
    private List<String> args;
    private Map<String, String> env;
    private String workingDirectory;

    // HTTP config (for future)
    private String url;
    private Map<String, String> headers;

    // Timeouts
    private int connectionTimeoutMs = 30000;
    private int requestTimeoutMs = 60000;

    /**
     * Transport types for MCP servers.
     */
    public enum TransportType {
        /** Local process via stdin/stdout */
        STDIO,
        /** Remote server via HTTP/SSE */
        HTTP
    }

    /**
     * Creates an empty configuration.
     */
    public McpServerConfig() {
        this.id = UUID.randomUUID().toString();
        this.enabled = true;
        this.transportType = TransportType.STDIO;
        this.args = new ArrayList<>();
        this.env = new HashMap<>();
    }

    /**
     * Returns the unique ID.
     *
     * @return the ID
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the display name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns whether the server is enabled.
     *
     * @return true if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Returns the transport type.
     *
     * @return the transport type
     */
    public TransportType getTransportType() {
        return transportType;
    }

    /**
     * Returns the command to execute.
     *
     * @return the command
     */
    public String getCommand() {
        return command;
    }

    /**
     * Returns the command arguments.
     *
     * @return the arguments (never null)
     */
    public List<String> getArgs() {
        return args != null ? args : Collections.emptyList();
    }

    /**
     * Returns the environment variables.
     *
     * @return the environment (never null)
     */
    public Map<String, String> getEnv() {
        return env != null ? env : Collections.emptyMap();
    }

    /**
     * Returns the working directory.
     *
     * @return the working directory, may be null
     */
    public String getWorkingDirectory() {
        return workingDirectory;
    }

    /**
     * Returns the connection timeout in milliseconds.
     *
     * @return the connection timeout
     */
    public int getConnectionTimeoutMs() {
        return connectionTimeoutMs;
    }

    /**
     * Returns the request timeout in milliseconds.
     *
     * @return the request timeout
     */
    public int getRequestTimeoutMs() {
        return requestTimeoutMs;
    }

    /**
     * Returns the URL for HTTP transport.
     *
     * @return the URL
     */
    public String getUrl() {
        return url;
    }

    /**
     * Returns the HTTP headers.
     *
     * @return the headers
     */
    public Map<String, String> getHeaders() {
        return headers != null ? headers : Collections.emptyMap();
    }

    /**
     * Validates the configuration.
     *
     * @return true if valid
     */
    public boolean isValid() {
        if (name == null || name.isBlank()) {
            return false;
        }
        if (transportType == TransportType.STDIO) {
            return command != null && !command.isBlank();
        } else {
            return url != null && !url.isBlank();
        }
    }

    /**
     * Converts this configuration to JSON.
     *
     * @return the JSON object
     */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("name", name);
        json.addProperty("enabled", enabled);
        json.addProperty("transportType", transportType.name());
        json.addProperty("command", command);
        json.add("args", new Gson().toJsonTree(args));
        json.add("env", new Gson().toJsonTree(env));
        if (workingDirectory != null) {
            json.addProperty("workingDirectory", workingDirectory);
        }
        json.addProperty("connectionTimeoutMs", connectionTimeoutMs);
        json.addProperty("requestTimeoutMs", requestTimeoutMs);
        return json;
    }

    /**
     * Creates a configuration from JSON.
     *
     * @param json the JSON object
     * @return the configuration
     */
    public static McpServerConfig fromJson(JsonObject json) {
        Builder builder = builder()
            .id(json.has("id") ? json.get("id").getAsString() : null)
            .name(json.get("name").getAsString())
            .enabled(json.has("enabled") ? json.get("enabled").getAsBoolean() : true)
            .command(json.has("command") ? json.get("command").getAsString() : null);

        if (json.has("transportType")) {
            try {
                builder.transportType(TransportType.valueOf(json.get("transportType").getAsString()));
            } catch (IllegalArgumentException e) {
                // Default to STDIO
            }
        }

        if (json.has("args") && json.get("args").isJsonArray()) {
            JsonArray argsArr = json.getAsJsonArray("args");
            argsArr.forEach(e -> builder.addArg(e.getAsString()));
        }

        if (json.has("env") && json.get("env").isJsonObject()) {
            JsonObject envObj = json.getAsJsonObject("env");
            envObj.entrySet().forEach(e -> builder.putEnv(e.getKey(), e.getValue().getAsString()));
        }

        if (json.has("workingDirectory") && !json.get("workingDirectory").isJsonNull()) {
            builder.workingDirectory(json.get("workingDirectory").getAsString());
        }

        if (json.has("connectionTimeoutMs")) {
            builder.connectionTimeout(json.get("connectionTimeoutMs").getAsInt());
        }

        if (json.has("requestTimeoutMs")) {
            builder.requestTimeout(json.get("requestTimeoutMs").getAsInt());
        }

        return builder.build();
    }

    /**
     * Creates a new builder.
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        McpServerConfig other = (McpServerConfig) obj;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "McpServerConfig[id=" + id + ", name=" + name + ", command=" + command + "]";
    }

    /**
     * Builder for MCP server configuration.
     */
    public static class Builder {

        private final McpServerConfig config = new McpServerConfig();

        /**
         * Sets the ID.
         *
         * @param id the ID
         * @return this builder
         */
        public Builder id(String id) {
            if (id != null && !id.isBlank()) {
                config.id = id;
            }
            return this;
        }

        /**
         * Sets the name.
         *
         * @param name the name
         * @return this builder
         */
        public Builder name(String name) {
            config.name = name;
            return this;
        }

        /**
         * Sets whether enabled.
         *
         * @param enabled true if enabled
         * @return this builder
         */
        public Builder enabled(boolean enabled) {
            config.enabled = enabled;
            return this;
        }

        /**
         * Sets the transport type.
         *
         * @param type the transport type
         * @return this builder
         */
        public Builder transportType(TransportType type) {
            config.transportType = type;
            return this;
        }

        /**
         * Sets the command.
         *
         * @param command the command
         * @return this builder
         */
        public Builder command(String command) {
            config.command = command;
            return this;
        }

        /**
         * Sets the arguments.
         *
         * @param args the arguments
         * @return this builder
         */
        public Builder args(List<String> args) {
            config.args = new ArrayList<>(args);
            return this;
        }

        /**
         * Adds an argument.
         *
         * @param arg the argument
         * @return this builder
         */
        public Builder addArg(String arg) {
            config.args.add(arg);
            return this;
        }

        /**
         * Sets the environment.
         *
         * @param env the environment
         * @return this builder
         */
        public Builder env(Map<String, String> env) {
            config.env = new HashMap<>(env);
            return this;
        }

        /**
         * Adds an environment variable.
         *
         * @param key the key
         * @param value the value
         * @return this builder
         */
        public Builder putEnv(String key, String value) {
            config.env.put(key, value);
            return this;
        }

        /**
         * Sets the working directory.
         *
         * @param dir the directory
         * @return this builder
         */
        public Builder workingDirectory(String dir) {
            config.workingDirectory = dir;
            return this;
        }

        /**
         * Sets the connection timeout.
         *
         * @param ms the timeout in milliseconds
         * @return this builder
         */
        public Builder connectionTimeout(int ms) {
            config.connectionTimeoutMs = ms;
            return this;
        }

        /**
         * Sets the request timeout.
         *
         * @param ms the timeout in milliseconds
         * @return this builder
         */
        public Builder requestTimeout(int ms) {
            config.requestTimeoutMs = ms;
            return this;
        }

        /**
         * Builds the configuration.
         *
         * @return the configuration
         */
        public McpServerConfig build() {
            Objects.requireNonNull(config.name, "name is required");
            Objects.requireNonNull(config.command, "command is required");
            return config;
        }
    }
}
