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

import com.google.gson.annotations.SerializedName;

/**
 * Represents the result of an MCP prompt/get operation.
 */
public class McpPromptResult {

    @SerializedName("description")
    private String description;

    @SerializedName("messages")
    private List<PromptMessage> messages;

    /**
     * Creates an empty prompt result.
     */
    public McpPromptResult() {
        this.messages = new ArrayList<>();
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
     * Returns the prompt messages.
     *
     * @return the messages
     */
    public List<PromptMessage> getMessages() {
        return messages != null ? messages : Collections.emptyList();
    }

    /**
     * Sets the prompt messages.
     *
     * @param messages the messages
     */
    public void setMessages(List<PromptMessage> messages) {
        this.messages = messages;
    }

    /**
     * A message within a prompt result.
     */
    public static class PromptMessage {

        @SerializedName("role")
        private String role;

        @SerializedName("content")
        private McpContent content;

        /**
         * Creates an empty prompt message.
         */
        public PromptMessage() {
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public McpContent getContent() {
            return content;
        }

        public void setContent(McpContent content) {
            this.content = content;
        }
    }

    @Override
    public String toString() {
        return "McpPromptResult[messages=" + getMessages().size() + "]";
    }
}
