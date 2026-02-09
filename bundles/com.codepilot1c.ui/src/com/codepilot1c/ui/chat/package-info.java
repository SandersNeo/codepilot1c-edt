/*
 * Copyright (c) 2024 Example
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 * SPDX-License-Identifier: AGPL-3.0-only
 */
/**
 * Chat UI model, session management, and agent integration.
 *
 * <p>This package provides UI-layer abstractions for chat conversations,
 * decoupled from the core LLM API types:</p>
 * <ul>
 *   <li>{@link com.codepilot1c.ui.chat.ChatMessage} - UI message with id, timestamp, status</li>
 *   <li>{@link com.codepilot1c.ui.chat.ChatSession} - Conversation management with editing/regeneration</li>
 *   <li>{@link com.codepilot1c.ui.chat.MessagePart} - Structured message content (text, code, tools, todos)</li>
 *   <li>{@link com.codepilot1c.ui.chat.MessageContentParser} - Parses Markdown into MessageParts</li>
 * </ul>
 *
 * <h2>Agent Integration</h2>
 * <ul>
 *   <li>{@link com.codepilot1c.ui.chat.AgentViewAdapter} - Adapter for AgentRunner to UI updates</li>
 *   <li>{@link com.codepilot1c.ui.chat.AgentProgressWidget} - Widget for agent progress display</li>
 * </ul>
 *
 * @see com.codepilot1c.core.agent.AgentRunner
 */
package com.codepilot1c.ui.chat;
