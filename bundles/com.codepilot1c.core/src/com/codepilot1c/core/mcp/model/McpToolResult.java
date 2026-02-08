/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
 */
package com.codepilot1c.core.mcp.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gson.annotations.SerializedName;

/**
 * Represents the result of an MCP tool call.
 */
public class McpToolResult {

    @SerializedName("content")
    private List<McpContent> content;

    @SerializedName("isError")
    private boolean isError;

    /**
     * Creates an empty tool result.
     */
    public McpToolResult() {
        this.content = new ArrayList<>();
    }

    /**
     * Creates a successful text result.
     *
     * @param text the result text
     * @return the tool result
     */
    public static McpToolResult text(String text) {
        McpToolResult result = new McpToolResult();
        result.content.add(McpContent.text(text));
        result.isError = false;
        return result;
    }

    /**
     * Creates an error result.
     *
     * @param errorMessage the error message
     * @return the tool result
     */
    public static McpToolResult error(String errorMessage) {
        McpToolResult result = new McpToolResult();
        result.content.add(McpContent.text(errorMessage));
        result.isError = true;
        return result;
    }

    /**
     * Returns the content list.
     *
     * @return the content
     */
    public List<McpContent> getContent() {
        return content != null ? content : Collections.emptyList();
    }

    /**
     * Sets the content list.
     *
     * @param content the content
     */
    public void setContent(List<McpContent> content) {
        this.content = content;
    }

    /**
     * Returns whether this is an error result.
     *
     * @return true if error
     */
    public boolean isError() {
        return isError;
    }

    /**
     * Sets whether this is an error result.
     *
     * @param isError true if error
     */
    public void setError(boolean isError) {
        this.isError = isError;
    }

    /**
     * Extracts all text content as a single string.
     *
     * @return concatenated text content
     */
    public String getTextContent() {
        StringBuilder sb = new StringBuilder();
        for (McpContent c : getContent()) {
            if (c.getType() == McpContent.Type.TEXT && c.getText() != null) {
                if (sb.length() > 0) {
                    sb.append("\n");
                }
                sb.append(c.getText());
            }
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "McpToolResult[isError=" + isError + ", contentCount=" + getContent().size() + "]";
    }
}
