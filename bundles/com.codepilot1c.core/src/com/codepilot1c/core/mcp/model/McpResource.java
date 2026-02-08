/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
 */
package com.codepilot1c.core.mcp.model;

import java.util.Objects;

import com.google.gson.annotations.SerializedName;

/**
 * Represents an MCP resource definition.
 *
 * <p>Resources are data items that can be read by the AI.</p>
 */
public class McpResource {

    @SerializedName("uri")
    private String uri;

    @SerializedName("name")
    private String name;

    @SerializedName("description")
    private String description;

    @SerializedName("mimeType")
    private String mimeType;

    /**
     * Creates an empty resource.
     */
    public McpResource() {
    }

    /**
     * Creates a resource with URI and name.
     *
     * @param uri the resource URI
     * @param name the resource name
     */
    public McpResource(String uri, String name) {
        this.uri = uri;
        this.name = name;
    }

    /**
     * Returns the resource URI.
     *
     * @return the URI
     */
    public String getUri() {
        return uri;
    }

    /**
     * Sets the resource URI.
     *
     * @param uri the URI
     */
    public void setUri(String uri) {
        this.uri = uri;
    }

    /**
     * Returns the resource name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the resource name.
     *
     * @param name the name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the resource description.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the resource description.
     *
     * @param description the description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns the resource MIME type.
     *
     * @return the MIME type
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * Sets the resource MIME type.
     *
     * @param mimeType the MIME type
     */
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        McpResource other = (McpResource) obj;
        return Objects.equals(uri, other.uri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uri);
    }

    @Override
    public String toString() {
        return "McpResource[uri=" + uri + ", name=" + name + "]";
    }
}
