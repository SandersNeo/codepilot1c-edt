/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
 */
package com.codepilot1c.core.agent;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.codepilot1c.core.agent.events.IAgentEventListener;
import com.codepilot1c.core.model.LlmMessage;

/**
 * Интерфейс для запуска и управления агентом.
 *
 * <p>AgentRunner выполняет agentic loop:</p>
 * <pre>
 * prompt → LLM → tool_calls → execute → tool_result → LLM → ... → final response
 * </pre>
 *
 * <p>Пример использования:</p>
 * <pre>
 * IAgentRunner runner = new AgentRunner(provider, toolRegistry);
 *
 * // Подписка на события
 * runner.addListener(event -> {
 *     if (event instanceof ToolCallEvent) {
 *         // Обновить UI
 *     }
 * });
 *
 * // Запуск
 * AgentConfig config = AgentConfig.builder()
 *     .maxSteps(25)
 *     .build();
 *
 * runner.run("Создай новый метод для обработки заказов", config)
 *     .thenAccept(result -> {
 *         if (result.isSuccess()) {
 *             System.out.println(result.getFinalResponse());
 *         }
 *     });
 *
 * // Отмена (если нужно)
 * runner.cancel();
 * </pre>
 */
public interface IAgentRunner {

    /**
     * Запускает агента с указанным промптом.
     *
     * @param prompt пользовательский запрос
     * @param config конфигурация агента
     * @return future с результатом выполнения
     * @throws IllegalStateException если агент уже выполняется
     */
    CompletableFuture<AgentResult> run(String prompt, AgentConfig config);

    /**
     * Запускает агента с историей сообщений (продолжение диалога).
     *
     * @param prompt новый пользовательский запрос
     * @param history предыдущая история сообщений
     * @param config конфигурация агента
     * @return future с результатом выполнения
     * @throws IllegalStateException если агент уже выполняется
     */
    CompletableFuture<AgentResult> run(String prompt, List<LlmMessage> history, AgentConfig config);

    /**
     * Отменяет текущее выполнение агента.
     *
     * <p>Отмена асинхронная - агент может выполнить еще несколько
     * операций перед полной остановкой.</p>
     */
    void cancel();

    /**
     * Возвращает текущее состояние агента.
     *
     * @return текущее состояние
     */
    AgentState getState();

    /**
     * Проверяет, выполняется ли агент в данный момент.
     *
     * @return true если агент активен
     */
    default boolean isRunning() {
        return getState().isActive();
    }

    /**
     * Добавляет слушателя событий агента.
     *
     * @param listener слушатель
     */
    void addListener(IAgentEventListener listener);

    /**
     * Удаляет слушателя событий агента.
     *
     * @param listener слушатель
     */
    void removeListener(IAgentEventListener listener);

    /**
     * Возвращает количество выполненных шагов в текущем запуске.
     *
     * @return количество шагов
     */
    int getCurrentStep();

    /**
     * Возвращает историю сообщений текущего запуска.
     *
     * @return список сообщений (может быть пустым)
     */
    List<LlmMessage> getConversationHistory();

    /**
     * Освобождает ресурсы агента.
     * После вызова агент нельзя использовать.
     */
    void dispose();
}
