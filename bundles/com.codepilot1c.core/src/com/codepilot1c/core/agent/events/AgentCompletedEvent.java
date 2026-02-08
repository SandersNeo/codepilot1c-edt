/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
 */
package com.codepilot1c.core.agent.events;

import com.codepilot1c.core.agent.AgentResult;
import com.codepilot1c.core.agent.AgentState;

/**
 * Событие завершения выполнения агента.
 */
public class AgentCompletedEvent extends AgentEvent {

    private final AgentResult result;

    /**
     * Создает событие завершения.
     *
     * @param result результат выполнения
     */
    public AgentCompletedEvent(AgentResult result) {
        super(result.getFinalState(), result.getStepsExecuted());
        this.result = result;
    }

    /**
     * Результат выполнения агента.
     *
     * @return результат
     */
    public AgentResult getResult() {
        return result;
    }

    /**
     * Успешно ли завершился агент.
     *
     * @return true если успешно
     */
    public boolean isSuccess() {
        return result.isSuccess();
    }

    /**
     * Был ли агент отменен.
     *
     * @return true если отменен
     */
    public boolean isCancelled() {
        return result.isCancelled();
    }

    /**
     * Завершился ли агент с ошибкой.
     *
     * @return true если ошибка
     */
    public boolean isError() {
        return result.isError();
    }

    /**
     * Финальный ответ (если есть).
     *
     * @return ответ или null
     */
    public String getFinalResponse() {
        return result.getFinalResponse();
    }

    /**
     * Сообщение об ошибке (если есть).
     *
     * @return сообщение или null
     */
    public String getErrorMessage() {
        return result.getErrorMessage();
    }

    @Override
    public EventType getType() {
        return EventType.COMPLETED;
    }

    @Override
    public String toString() {
        return "AgentCompletedEvent{" +
                "state=" + getState() +
                ", steps=" + result.getStepsExecuted() +
                ", toolCalls=" + result.getToolCallsExecuted() +
                ", timeMs=" + result.getExecutionTimeMs() +
                '}';
    }
}
