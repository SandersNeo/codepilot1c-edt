/*
 * Copyright (c) 2024 Example
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 * SPDX-License-Identifier: AGPL-3.0-only
 */

/**
 * Управление сессиями диалогов с агентом.
 *
 * <h2>Основные компоненты</h2>
 * <ul>
 *   <li>{@link com.codepilot1c.core.session.Session} - модель сессии</li>
 *   <li>{@link com.codepilot1c.core.session.SessionMessage} - сообщение в сессии</li>
 *   <li>{@link com.codepilot1c.core.session.SessionManager} - центральный менеджер</li>
 *   <li>{@link com.codepilot1c.core.session.ISessionStore} - интерфейс хранилища</li>
 *   <li>{@link com.codepilot1c.core.session.FileSessionStore} - файловая реализация</li>
 * </ul>
 *
 * <h2>Использование</h2>
 * <pre>
 * // Получить менеджер
 * SessionManager manager = SessionManager.getInstance();
 *
 * // Создать сессию для проекта
 * Session session = manager.createSessionForProject(project);
 *
 * // Добавить сообщения
 * session.addMessage(SessionMessage.user("Привет!"));
 * session.addMessage(SessionMessage.assistant("Здравствуйте!"));
 *
 * // Сохранить
 * manager.saveSession(session);
 *
 * // Загрузить позже
 * Optional&lt;Session&gt; loaded = manager.loadSession(sessionId);
 *
 * // Конвертировать в LlmMessage для отправки провайдеру
 * List&lt;LlmMessage&gt; messages = session.toLlmMessages();
 * </pre>
 *
 * <h2>Хранение</h2>
 * <p>Сессии сохраняются в JSON файлы в директории плагина:</p>
 * <pre>
 * {plugin-state}/sessions/
 *   {session-id}.json
 *   ...
 * </pre>
 *
 * @see com.codepilot1c.core.agent.AgentRunner
 */
package com.codepilot1c.core.session;
