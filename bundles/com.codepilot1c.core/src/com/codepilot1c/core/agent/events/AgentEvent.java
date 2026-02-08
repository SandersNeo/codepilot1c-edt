/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
 */
package com.codepilot1c.core.agent.events;

import java.time.Instant;
import java.util.Objects;

import com.codepilot1c.core.agent.AgentState;

/**
 * Базовый класс для всех событий агента.
 *
 * <p>События используются для уведомления UI о ходе выполнения агента:</p>
 * <ul>
 *   <li>{@link AgentStartedEvent} - агент начал выполнение</li>
 *   <li>{@link AgentStepEvent} - агент выполнил шаг (итерацию loop)</li>
 *   <li>{@link ToolCallEvent} - агент вызвал инструмент</li>
 *   <li>{@link ToolResultEvent} - инструмент вернул результат</li>
 *   <li>{@link StreamChunkEvent} - получен chunk потокового ответа</li>
 *   <li>{@link AgentCompletedEvent} - агент завершил выполнение</li>
 * </ul>
 */
public abstract class AgentEvent {

    private final Instant timestamp;
    private final AgentState state;
    private final int step;

    /**
     * Создает событие.
     *
     * @param state текущее состояние агента
     * @param step текущий шаг
     */
    protected AgentEvent(AgentState state, int step) {
        this.timestamp = Instant.now();
        this.state = Objects.requireNonNull(state);
        this.step = step;
    }

    /**
     * Время создания события.
     *
     * @return timestamp
     */
    public Instant getTimestamp() {
        return timestamp;
    }

    /**
     * Состояние агента в момент события.
     *
     * @return состояние
     */
    public AgentState getState() {
        return state;
    }

    /**
     * Номер шага (итерации loop).
     *
     * @return номер шага (начиная с 1)
     */
    public int getStep() {
        return step;
    }

    /**
     * Тип события для паттерн-матчинга.
     *
     * @return тип события
     */
    public abstract EventType getType();

    /**
     * Типы событий агента.
     */
    public enum EventType {
        /** Агент начал выполнение */
        STARTED,
        /** Агент выполнил шаг */
        STEP,
        /** Агент вызвал инструмент */
        TOOL_CALL,
        /** Инструмент вернул результат */
        TOOL_RESULT,
        /** Получен chunk потокового ответа */
        STREAM_CHUNK,
        /** Требуется подтверждение пользователя */
        CONFIRMATION_REQUIRED,
        /** Агент завершил выполнение */
        COMPLETED
    }
}
