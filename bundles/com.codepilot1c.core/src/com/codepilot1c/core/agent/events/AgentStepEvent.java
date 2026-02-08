/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
 */
package com.codepilot1c.core.agent.events;

import com.codepilot1c.core.agent.AgentState;

/**
 * Событие выполнения шага агента (итерации loop).
 */
public class AgentStepEvent extends AgentEvent {

    private final int maxSteps;
    private final String description;

    /**
     * Создает событие шага.
     *
     * @param step текущий шаг
     * @param maxSteps максимальное количество шагов
     * @param description описание действия
     */
    public AgentStepEvent(int step, int maxSteps, String description) {
        super(AgentState.RUNNING, step);
        this.maxSteps = maxSteps;
        this.description = description;
    }

    /**
     * Максимальное количество шагов.
     *
     * @return max steps
     */
    public int getMaxSteps() {
        return maxSteps;
    }

    /**
     * Описание текущего действия.
     *
     * @return описание
     */
    public String getDescription() {
        return description;
    }

    /**
     * Прогресс выполнения (0.0 - 1.0).
     *
     * @return прогресс
     */
    public double getProgress() {
        return (double) getStep() / maxSteps;
    }

    @Override
    public EventType getType() {
        return EventType.STEP;
    }

    @Override
    public String toString() {
        return "AgentStepEvent{step=" + getStep() + "/" + maxSteps + ", desc='" + description + "'}";
    }
}
