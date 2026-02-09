/*
 * Copyright (c) 2024 Example
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 * SPDX-License-Identifier: AGPL-3.0-only
 */

/**
 * Пакет агента для выполнения agentic loop.
 *
 * <h2>Основные компоненты</h2>
 * <ul>
 *   <li>{@link com.codepilot1c.core.agent.IAgentRunner} - интерфейс запуска агента</li>
 *   <li>{@link com.codepilot1c.core.agent.AgentRunner} - реализация agentic loop</li>
 *   <li>{@link com.codepilot1c.core.agent.AgentConfig} - конфигурация агента</li>
 *   <li>{@link com.codepilot1c.core.agent.AgentState} - состояния агента</li>
 *   <li>{@link com.codepilot1c.core.agent.AgentResult} - результат выполнения</li>
 * </ul>
 *
 * <h2>Подпакеты</h2>
 * <ul>
 *   <li>{@code events} - события агента для UI</li>
 *   <li>{@code profiles} - профили агентов (build, plan, explore)</li>
 * </ul>
 *
 * <h2>Архитектура</h2>
 * <pre>
 *                  ┌─────────────┐
 *                  │  UI Layer   │
 *                  │  (ChatView) │
 *                  └──────┬──────┘
 *                         │ observe events
 *                         ▼
 *                  ┌─────────────┐
 *                  │ AgentRunner │ ◄── IAgentRunner
 *                  │   (loop)    │
 *                  └──────┬──────┘
 *                         │
 *          ┌──────────────┼──────────────┐
 *          ▼              ▼              ▼
 *   ┌────────────┐ ┌────────────┐ ┌────────────┐
 *   │LlmProvider │ │ToolRegistry│ │ Permission │
 *   │ (Claude,   │ │ (read,edit │ │  Manager   │
 *   │  OpenAI)   │ │  bash...)  │ │            │
 *   └────────────┘ └────────────┘ └────────────┘
 * </pre>
 *
 * @see com.codepilot1c.core.agent.events
 */
package com.codepilot1c.core.agent;
