/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
 */
package com.codepilot1c.core.model;

import java.util.Objects;

/**
 * Defines a tool that can be called by the LLM.
 *
 * <p>This follows the OpenAI/Anthropic function calling format.</p>
 */
public class ToolDefinition {

    private final String name;
    private final String description;
    private final String parametersSchema;

    /**
     * Creates a new tool definition.
     *
     * @param name the unique tool name
     * @param description description of what the tool does
     * @param parametersSchema JSON schema for the parameters
     */
    public ToolDefinition(String name, String description, String parametersSchema) {
        this.name = Objects.requireNonNull(name, "name"); //$NON-NLS-1$
        this.description = Objects.requireNonNull(description, "description"); //$NON-NLS-1$
        this.parametersSchema = Objects.requireNonNull(parametersSchema, "parametersSchema"); //$NON-NLS-1$
    }

    /**
     * Creates a tool definition using the builder.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getParametersSchema() {
        return parametersSchema;
    }

    /**
     * Builder for creating tool definitions.
     */
    public static class Builder {
        private String name;
        private String description;
        private String parametersSchema = "{}"; //$NON-NLS-1$

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder parametersSchema(String parametersSchema) {
            this.parametersSchema = parametersSchema;
            return this;
        }

        public ToolDefinition build() {
            return new ToolDefinition(name, description, parametersSchema);
        }
    }
}
