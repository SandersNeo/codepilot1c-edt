/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
 */
package com.codepilot1c.core.mcp.model;

import com.google.gson.annotations.SerializedName;

/**
 * Represents an argument definition for an MCP prompt.
 */
public class McpPromptArgument {

    @SerializedName("name")
    private String name;

    @SerializedName("description")
    private String description;

    @SerializedName("required")
    private boolean required;

    /**
     * Creates an empty prompt argument.
     */
    public McpPromptArgument() {
    }

    /**
     * Creates a prompt argument with name and description.
     *
     * @param name the argument name
     * @param description the argument description
     * @param required whether the argument is required
     */
    public McpPromptArgument(String name, String description, boolean required) {
        this.name = name;
        this.description = description;
        this.required = required;
    }

    /**
     * Returns the argument name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the argument name.
     *
     * @param name the name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the argument description.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the argument description.
     *
     * @param description the description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns whether the argument is required.
     *
     * @return true if required
     */
    public boolean isRequired() {
        return required;
    }

    /**
     * Sets whether the argument is required.
     *
     * @param required true if required
     */
    public void setRequired(boolean required) {
        this.required = required;
    }

    @Override
    public String toString() {
        return "McpPromptArgument[name=" + name + ", required=" + required + "]";
    }
}
