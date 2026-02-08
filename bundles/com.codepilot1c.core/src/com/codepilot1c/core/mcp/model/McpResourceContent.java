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
 * Represents the content of an MCP resource read operation.
 */
public class McpResourceContent {

    @SerializedName("contents")
    private List<ResourceContentItem> contents;

    /**
     * Creates an empty resource content.
     */
    public McpResourceContent() {
        this.contents = new ArrayList<>();
    }

    /**
     * Returns the content items.
     *
     * @return the contents
     */
    public List<ResourceContentItem> getContents() {
        return contents != null ? contents : Collections.emptyList();
    }

    /**
     * Sets the content items.
     *
     * @param contents the contents
     */
    public void setContents(List<ResourceContentItem> contents) {
        this.contents = contents;
    }

    /**
     * A single content item within a resource.
     */
    public static class ResourceContentItem {

        @SerializedName("uri")
        private String uri;

        @SerializedName("mimeType")
        private String mimeType;

        @SerializedName("text")
        private String text;

        @SerializedName("blob")
        private String blob;

        /**
         * Creates an empty content item.
         */
        public ResourceContentItem() {
        }

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public String getMimeType() {
            return mimeType;
        }

        public void setMimeType(String mimeType) {
            this.mimeType = mimeType;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public String getBlob() {
            return blob;
        }

        public void setBlob(String blob) {
            this.blob = blob;
        }
    }

    @Override
    public String toString() {
        return "McpResourceContent[items=" + getContents().size() + "]";
    }
}
