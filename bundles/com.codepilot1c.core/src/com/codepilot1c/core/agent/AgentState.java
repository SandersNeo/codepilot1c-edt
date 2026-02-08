/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
 */
package com.codepilot1c.core.agent;

/**
 * Состояние агента во время выполнения.
 *
 * <p>Диаграмма переходов состояний:</p>
 * <pre>
 * IDLE ──► RUNNING ──► WAITING_TOOL ──► RUNNING ──► COMPLETED
 *              │              │              │           │
 *              └──────────────┴──────────────┴───► CANCELLED
 *              │              │              │
 *              └──────────────┴──────────────┴───► ERROR
 * </pre>
 */
public enum AgentState {

    /**
     * Агент не запущен, готов к работе.
     */
    IDLE,

    /**
     * Агент выполняется, ожидает ответа от LLM.
     */
    RUNNING,

    /**
     * Агент ожидает выполнения инструмента.
     */
    WAITING_TOOL,

    /**
     * Агент ожидает подтверждения от пользователя
     * (для инструментов с requiresConfirmation=true).
     */
    WAITING_CONFIRMATION,

    /**
     * Агент успешно завершил работу.
     */
    COMPLETED,

    /**
     * Агент был отменен пользователем.
     */
    CANCELLED,

    /**
     * Агент завершился с ошибкой.
     */
    ERROR;

    /**
     * Проверяет, является ли состояние терминальным
     * (агент больше не выполняется).
     *
     * @return true если состояние терминальное
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED || this == ERROR;
    }

    /**
     * Проверяет, выполняется ли агент в данный момент.
     *
     * @return true если агент активен
     */
    public boolean isActive() {
        return this == RUNNING || this == WAITING_TOOL || this == WAITING_CONFIRMATION;
    }

    /**
     * Проверяет, ожидает ли агент внешнего действия.
     *
     * @return true если агент ожидает
     */
    public boolean isWaiting() {
        return this == WAITING_TOOL || this == WAITING_CONFIRMATION;
    }
}
