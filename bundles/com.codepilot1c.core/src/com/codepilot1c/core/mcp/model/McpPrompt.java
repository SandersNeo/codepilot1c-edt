/*
 * Copyright (c) 2024 Example
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.codepilot1c.core.mcp.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.google.gson.annotations.SerializedName;

/**
 * Represents an MCP prompt template.
 *
 * <p>Prompts are reusable prompt templates that can be parameterized.</p>
 */
public class McpPrompt {

    @SerializedName("name")
    private String name;

    @SerializedName("description")
    private String description;

    @SerializedName("arguments")
    private List<McpPromptArgument> arguments;

    /**
     * Creates an empty prompt.
     */
    public McpPrompt() {
        this.arguments = new ArrayList<>();
    }

    /**
     * Creates a prompt with name and description.
     *
     * @param name the prompt name
     * @param description the prompt description
     */
    public McpPrompt(String name, String description) {
        this.name = name;
        this.description = description;
        this.arguments = new ArrayList<>();
    }

    /**
     * Returns the prompt name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the prompt name.
     *
     * @param name the name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the prompt description.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the prompt description.
     *
     * @param description the description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns the prompt arguments.
     *
     * @return the arguments
     */
    public List<McpPromptArgument> getArguments() {
        return arguments != null ? arguments : Collections.emptyList();
    }

    /**
     * Sets the prompt arguments.
     *
     * @param arguments the arguments
     */
    public void setArguments(List<McpPromptArgument> arguments) {
        this.arguments = arguments;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        McpPrompt other = (McpPrompt) obj;
        return Objects.equals(name, other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "McpPrompt[name=" + name + "]";
    }
}
