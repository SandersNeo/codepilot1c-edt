/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
 */

/**
 * События агента для уведомления UI о ходе выполнения.
 *
 * <h2>Иерархия событий</h2>
 * <pre>
 * AgentEvent (abstract)
 *   ├── AgentStartedEvent      - начало выполнения
 *   ├── AgentStepEvent         - выполнение шага (итерации)
 *   ├── ToolCallEvent          - вызов инструмента
 *   ├── ToolResultEvent        - результат инструмента
 *   ├── StreamChunkEvent       - chunk потокового ответа
 *   ├── ConfirmationRequiredEvent - требуется подтверждение
 *   └── AgentCompletedEvent    - завершение выполнения
 * </pre>
 *
 * <h2>Использование</h2>
 * <pre>
 * agentRunner.addListener(event -> {
 *     Display.getDefault().asyncExec(() -> {
 *         switch (event.getType()) {
 *             case STREAM_CHUNK:
 *                 appendText(((StreamChunkEvent) event).getContent());
 *                 break;
 *             case TOOL_CALL:
 *                 showToolProgress(((ToolCallEvent) event).getToolName());
 *                 break;
 *             case CONFIRMATION_REQUIRED:
 *                 ConfirmationRequiredEvent conf = (ConfirmationRequiredEvent) event;
 *                 if (showConfirmDialog(conf)) {
 *                     conf.confirm();
 *                 } else {
 *                     conf.deny();
 *                 }
 *                 break;
 *             // ...
 *         }
 *     });
 * });
 * </pre>
 *
 * @see com.codepilot1c.core.agent.IAgentRunner#addListener(IAgentEventListener)
 */
package com.codepilot1c.core.agent.events;
