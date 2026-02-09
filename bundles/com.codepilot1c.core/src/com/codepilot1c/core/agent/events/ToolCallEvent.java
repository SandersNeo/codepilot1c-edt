/*
 * Copyright (c) 2024 Example
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.codepilot1c.core.agent.events;

import java.util.Map;

import com.codepilot1c.core.agent.AgentState;
import com.codepilot1c.core.model.ToolCall;

/**
 * Событие вызова инструмента агентом.
 */
public class ToolCallEvent extends AgentEvent {

    private final ToolCall toolCall;
    private final Map<String, Object> parsedArguments;
    private final boolean requiresConfirmation;

    /**
     * Создает событие вызова инструмента.
     *
     * @param step текущий шаг
     * @param toolCall объект вызова инструмента
     * @param parsedArguments распарсенные аргументы
     * @param requiresConfirmation требуется ли подтверждение
     */
    public ToolCallEvent(int step, ToolCall toolCall,
                         Map<String, Object> parsedArguments,
                         boolean requiresConfirmation) {
        super(AgentState.WAITING_TOOL, step);
        this.toolCall = toolCall;
        this.parsedArguments = parsedArguments;
        this.requiresConfirmation = requiresConfirmation;
    }

    /**
     * Объект вызова инструмента.
     *
     * @return tool call
     */
    public ToolCall getToolCall() {
        return toolCall;
    }

    /**
     * Имя инструмента.
     *
     * @return имя
     */
    public String getToolName() {
        return toolCall.getName();
    }

    /**
     * ID вызова инструмента.
     *
     * @return id
     */
    public String getCallId() {
        return toolCall.getId();
    }

    /**
     * Распарсенные аргументы.
     *
     * @return аргументы
     */
    public Map<String, Object> getParsedArguments() {
        return parsedArguments;
    }

    /**
     * Требуется ли подтверждение пользователя.
     *
     * @return true если требуется
     */
    public boolean isRequiresConfirmation() {
        return requiresConfirmation;
    }

    @Override
    public EventType getType() {
        return EventType.TOOL_CALL;
    }

    @Override
    public String toString() {
        return "ToolCallEvent{tool='" + getToolName() + "', confirmation=" + requiresConfirmation + "}";
    }
}
