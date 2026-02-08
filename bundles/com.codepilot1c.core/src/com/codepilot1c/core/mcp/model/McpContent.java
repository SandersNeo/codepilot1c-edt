/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
 */
package com.codepilot1c.core.mcp.model;

import com.google.gson.annotations.SerializedName;

/**
 * Represents content in MCP protocol responses.
 *
 * <p>Content can be text, image (base64), or a resource reference.</p>
 */
public class McpContent {

    /**
     * Type of MCP content.
     */
    public enum Type {
        @SerializedName("text")
        TEXT,
        @SerializedName("image")
        IMAGE,
        @SerializedName("resource")
        RESOURCE
    }

    @SerializedName("type")
    private Type type;

    @SerializedName("text")
    private String text;

    @SerializedName("mimeType")
    private String mimeType;

    @SerializedName("data")
    private String data;

    @SerializedName("uri")
    private String uri;

    /**
     * Creates an empty content object.
     */
    public McpContent() {
    }

    /**
     * Creates a text content.
     *
     * @param text the text content
     * @return the content object
     */
    public static McpContent text(String text) {
        McpContent content = new McpContent();
        content.type = Type.TEXT;
        content.text = text;
        return content;
    }

    /**
     * Creates an image content.
     *
     * @param data the base64-encoded image data
     * @param mimeType the image MIME type
     * @return the content object
     */
    public static McpContent image(String data, String mimeType) {
        McpContent content = new McpContent();
        content.type = Type.IMAGE;
        content.data = data;
        content.mimeType = mimeType;
        return content;
    }

    /**
     * Creates a resource reference content.
     *
     * @param uri the resource URI
     * @param text the resource text (optional)
     * @param mimeType the resource MIME type (optional)
     * @return the content object
     */
    public static McpContent resource(String uri, String text, String mimeType) {
        McpContent content = new McpContent();
        content.type = Type.RESOURCE;
        content.uri = uri;
        content.text = text;
        content.mimeType = mimeType;
        return content;
    }

    // Getters

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    @Override
    public String toString() {
        return "McpContent[type=" + type + "]";
    }
}
