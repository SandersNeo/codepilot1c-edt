/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
 */
package com.codepilot1c.core.agent.events;

import com.codepilot1c.core.agent.AgentState;
import com.codepilot1c.core.tools.ToolResult;

/**
 * Событие результата выполнения инструмента.
 */
public class ToolResultEvent extends AgentEvent {

    private final String toolName;
    private final String callId;
    private final ToolResult result;
    private final long executionTimeMs;

    /**
     * Создает событие результата инструмента.
     *
     * @param step текущий шаг
     * @param toolName имя инструмента
     * @param callId ID вызова
     * @param result результат выполнения
     * @param executionTimeMs время выполнения в мс
     */
    public ToolResultEvent(int step, String toolName, String callId,
                           ToolResult result, long executionTimeMs) {
        super(AgentState.RUNNING, step);
        this.toolName = toolName;
        this.callId = callId;
        this.result = result;
        this.executionTimeMs = executionTimeMs;
    }

    /**
     * Имя инструмента.
     *
     * @return имя
     */
    public String getToolName() {
        return toolName;
    }

    /**
     * ID вызова инструмента.
     *
     * @return id
     */
    public String getCallId() {
        return callId;
    }

    /**
     * Результат выполнения.
     *
     * @return результат
     */
    public ToolResult getResult() {
        return result;
    }

    /**
     * Успешно ли выполнен инструмент.
     *
     * @return true если успешно
     */
    public boolean isSuccess() {
        return result.isSuccess();
    }

    /**
     * Время выполнения в миллисекундах.
     *
     * @return время в мс
     */
    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    @Override
    public EventType getType() {
        return EventType.TOOL_RESULT;
    }

    @Override
    public String toString() {
        return "ToolResultEvent{tool='" + toolName + "', success=" + isSuccess() +
                ", timeMs=" + executionTimeMs + "}";
    }
}
