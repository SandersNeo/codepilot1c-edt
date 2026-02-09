/*
 * Copyright (c) 2024 Example
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 * SPDX-License-Identifier: AGPL-3.0-only
 */
/**
 * MCP (Model Context Protocol) integration.
 *
 * <p>This package provides integration with MCP servers:</p>
 * <ul>
 *   <li>{@link com.codepilot1c.core.mcp.McpServerManager} - Server lifecycle management</li>
 *   <li>{@link com.codepilot1c.core.mcp.McpToolAdapter} - Adapts MCP tools to ITool</li>
 *   <li>{@link com.codepilot1c.core.mcp.IMcpServerListener} - Server events</li>
 * </ul>
 *
 * <p>Sub-packages:</p>
 * <ul>
 *   <li>{@code model} - MCP protocol data types</li>
 *   <li>{@code transport} - Transport implementations (STDIO)</li>
 *   <li>{@code client} - High-level MCP client</li>
 *   <li>{@code config} - Server configuration</li>
 * </ul>
 */
package com.codepilot1c.core.mcp;
