/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
 */
package com.codepilot1c.core.agent.events;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.codepilot1c.core.agent.AgentState;
import com.codepilot1c.core.model.ToolCall;

/**
 * Событие требования подтверждения от пользователя.
 *
 * <p>Используется для инструментов с {@code requiresConfirmation=true}
 * (например, редактирование файлов, выполнение shell команд).</p>
 *
 * <p>UI должен показать диалог подтверждения и вызвать
 * {@link #confirm()} или {@link #deny()} для продолжения.</p>
 */
public class ConfirmationRequiredEvent extends AgentEvent {

    /**
     * Результат подтверждения пользователя.
     */
    public enum ConfirmationResult {
        /** Пользователь подтвердил */
        CONFIRMED,
        /** Пользователь отклонил */
        DENIED,
        /** Пользователь пропустил (skip) */
        SKIPPED
    }

    private final ToolCall toolCall;
    private final String toolDescription;
    private final Map<String, Object> arguments;
    private final boolean isDestructive;
    private final CompletableFuture<ConfirmationResult> resultFuture;

    /**
     * Создает событие требования подтверждения.
     *
     * @param step текущий шаг
     * @param toolCall вызов инструмента
     * @param toolDescription описание инструмента
     * @param arguments аргументы вызова
     * @param isDestructive может ли инструмент изменить данные
     */
    public ConfirmationRequiredEvent(int step, ToolCall toolCall,
                                      String toolDescription,
                                      Map<String, Object> arguments,
                                      boolean isDestructive) {
        super(AgentState.WAITING_CONFIRMATION, step);
        this.toolCall = toolCall;
        this.toolDescription = toolDescription;
        this.arguments = arguments;
        this.isDestructive = isDestructive;
        this.resultFuture = new CompletableFuture<>();
    }

    /**
     * Вызов инструмента, требующий подтверждения.
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
     * Описание инструмента для показа пользователю.
     *
     * @return описание
     */
    public String getToolDescription() {
        return toolDescription;
    }

    /**
     * Аргументы вызова.
     *
     * @return аргументы
     */
    public Map<String, Object> getArguments() {
        return arguments;
    }

    /**
     * Может ли инструмент изменить или удалить данные.
     *
     * @return true если деструктивный
     */
    public boolean isDestructive() {
        return isDestructive;
    }

    /**
     * Подтвердить выполнение.
     */
    public void confirm() {
        resultFuture.complete(ConfirmationResult.CONFIRMED);
    }

    /**
     * Отклонить выполнение.
     */
    public void deny() {
        resultFuture.complete(ConfirmationResult.DENIED);
    }

    /**
     * Пропустить выполнение (продолжить без выполнения).
     */
    public void skip() {
        resultFuture.complete(ConfirmationResult.SKIPPED);
    }

    /**
     * Future для ожидания результата подтверждения.
     *
     * @return future
     */
    public CompletableFuture<ConfirmationResult> getResultFuture() {
        return resultFuture;
    }

    @Override
    public EventType getType() {
        return EventType.CONFIRMATION_REQUIRED;
    }

    @Override
    public String toString() {
        return "ConfirmationRequiredEvent{tool='" + getToolName() +
                "', destructive=" + isDestructive + "}";
    }
}
