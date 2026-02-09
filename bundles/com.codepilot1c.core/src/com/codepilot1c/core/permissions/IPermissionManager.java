/*
 * Copyright (c) 2024 Example
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.codepilot1c.core.permissions;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Интерфейс менеджера разрешений.
 *
 * <p>Управляет правилами разрешений для инструментов агента.
 * Позволяет проверять, разрешено ли действие, и запрашивать
 * подтверждение у пользователя при необходимости.</p>
 *
 * <h2>Использование</h2>
 * <pre>
 * IPermissionManager pm = PermissionManager.getInstance();
 *
 * // Проверка разрешения
 * CompletableFuture&lt;PermissionDecision&gt; decision =
 *     pm.check("shell", "execute", Map.of("command", "git status"));
 *
 * decision.thenAccept(d -> {
 *     if (d.isAllowed()) {
 *         // Выполнить команду
 *     } else {
 *         // Отклонено
 *     }
 * });
 *
 * // Добавление правила
 * pm.addRule(PermissionRule.allow("read_file").forAllResources());
 * </pre>
 */
public interface IPermissionManager {

    /**
     * Проверяет разрешение для действия.
     *
     * <p>Если правило возвращает ASK, будет вызван callback для
     * запроса подтверждения у пользователя.</p>
     *
     * @param toolName имя инструмента
     * @param action действие (например, "execute", "read", "write")
     * @param context дополнительный контекст (путь файла, команда и т.д.)
     * @return future с решением
     */
    CompletableFuture<PermissionDecision> check(
            String toolName,
            String action,
            Map<String, Object> context);

    /**
     * Проверяет разрешение синхронно (только по правилам, без ASK).
     *
     * @param toolName имя инструмента
     * @param resource ресурс (путь файла, команда)
     * @return решение (ALLOW, DENY или ASK)
     */
    PermissionDecision checkSync(String toolName, String resource);

    /**
     * Возвращает все правила для инструмента.
     *
     * @param toolName имя инструмента
     * @return список правил
     */
    List<PermissionRule> getRulesForTool(String toolName);

    /**
     * Возвращает все правила.
     *
     * @return список всех правил
     */
    List<PermissionRule> getAllRules();

    /**
     * Добавляет правило.
     *
     * @param rule правило
     */
    void addRule(PermissionRule rule);

    /**
     * Удаляет правило.
     *
     * @param rule правило
     * @return true если правило было удалено
     */
    boolean removeRule(PermissionRule rule);

    /**
     * Очищает все правила.
     */
    void clearRules();

    /**
     * Загружает правила по умолчанию.
     */
    void loadDefaultRules();

    /**
     * Устанавливает callback для запроса подтверждения.
     *
     * @param callback callback или null
     */
    void setCallback(IPermissionCallback callback);

    /**
     * Возвращает текущий callback.
     *
     * @return callback или null
     */
    IPermissionCallback getCallback();

    /**
     * Добавляет временное разрешение на сессию.
     *
     * @param toolName имя инструмента
     * @param resource ресурс
     * @param decision решение
     */
    void addSessionPermission(String toolName, String resource, PermissionDecision decision);

    /**
     * Очищает временные разрешения сессии.
     */
    void clearSessionPermissions();

    /**
     * Сохраняет правила в preferences.
     */
    void saveRules();

    /**
     * Загружает правила из preferences.
     */
    void loadRules();
}
