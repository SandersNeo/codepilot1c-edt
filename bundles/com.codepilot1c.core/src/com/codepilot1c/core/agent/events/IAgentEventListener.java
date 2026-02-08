/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
 */
package com.codepilot1c.core.agent.events;

/**
 * Слушатель событий агента.
 *
 * <p>Реализуйте этот интерфейс для получения уведомлений
 * о ходе выполнения агента.</p>
 *
 * <p>Пример использования:</p>
 * <pre>
 * agentRunner.addListener(event -> {
 *     switch (event.getType()) {
 *         case STARTED:
 *             AgentStartedEvent started = (AgentStartedEvent) event;
 *             showProgress("Начало: " + started.getPrompt());
 *             break;
 *
 *         case TOOL_CALL:
 *             ToolCallEvent toolCall = (ToolCallEvent) event;
 *             showProgress("Вызов: " + toolCall.getToolName());
 *             break;
 *
 *         case STREAM_CHUNK:
 *             StreamChunkEvent chunk = (StreamChunkEvent) event;
 *             appendText(chunk.getContent());
 *             break;
 *
 *         case COMPLETED:
 *             AgentCompletedEvent completed = (AgentCompletedEvent) event;
 *             showResult(completed.getResult());
 *             break;
 *     }
 * });
 * </pre>
 *
 * <p><b>Важно:</b> События могут приходить из фонового потока.
 * Для обновления UI используйте {@code Display.asyncExec()}.</p>
 */
@FunctionalInterface
public interface IAgentEventListener {

    /**
     * Вызывается при возникновении события агента.
     *
     * <p>Метод может быть вызван из любого потока.
     * Не выполняйте длительные операции в этом методе.</p>
     *
     * @param event событие агента
     */
    void onEvent(AgentEvent event);
}
