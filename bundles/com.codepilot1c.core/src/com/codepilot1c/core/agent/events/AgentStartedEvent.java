/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
 */
package com.codepilot1c.core.agent.events;

import com.codepilot1c.core.agent.AgentConfig;
import com.codepilot1c.core.agent.AgentState;

/**
 * Событие начала выполнения агента.
 */
public class AgentStartedEvent extends AgentEvent {

    private final String prompt;
    private final AgentConfig config;

    /**
     * Создает событие начала выполнения.
     *
     * @param prompt пользовательский запрос
     * @param config конфигурация агента
     */
    public AgentStartedEvent(String prompt, AgentConfig config) {
        super(AgentState.RUNNING, 0);
        this.prompt = prompt;
        this.config = config;
    }

    /**
     * Пользовательский запрос.
     *
     * @return промпт
     */
    public String getPrompt() {
        return prompt;
    }

    /**
     * Конфигурация агента.
     *
     * @return конфигурация
     */
    public AgentConfig getConfig() {
        return config;
    }

    @Override
    public EventType getType() {
        return EventType.STARTED;
    }

    @Override
    public String toString() {
        return "AgentStartedEvent{prompt='" + prompt.substring(0, Math.min(50, prompt.length())) + "...'}";
    }
}
