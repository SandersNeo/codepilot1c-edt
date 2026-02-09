/*
 * Copyright (c) 2024 Example
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.codepilot1c.core.session;

import java.util.List;
import java.util.Optional;

/**
 * Интерфейс для хранилища сессий.
 *
 * <p>Позволяет сохранять, загружать и управлять сессиями диалогов.</p>
 */
public interface ISessionStore {

    /**
     * Сохраняет сессию.
     *
     * @param session сессия для сохранения
     * @throws SessionStoreException при ошибке сохранения
     */
    void save(Session session) throws SessionStoreException;

    /**
     * Загружает сессию по ID.
     *
     * @param sessionId идентификатор сессии
     * @return сессия или empty если не найдена
     * @throws SessionStoreException при ошибке загрузки
     */
    Optional<Session> load(String sessionId) throws SessionStoreException;

    /**
     * Удаляет сессию по ID.
     *
     * @param sessionId идентификатор сессии
     * @return true если сессия была удалена
     * @throws SessionStoreException при ошибке удаления
     */
    boolean delete(String sessionId) throws SessionStoreException;

    /**
     * Проверяет, существует ли сессия.
     *
     * @param sessionId идентификатор сессии
     * @return true если существует
     */
    boolean exists(String sessionId);

    /**
     * Возвращает список всех сессий (краткая информация).
     *
     * @return список сессий
     * @throws SessionStoreException при ошибке
     */
    List<SessionSummary> listAll() throws SessionStoreException;

    /**
     * Возвращает список сессий для проекта.
     *
     * @param projectPath путь к проекту
     * @return список сессий
     * @throws SessionStoreException при ошибке
     */
    List<SessionSummary> listByProject(String projectPath) throws SessionStoreException;

    /**
     * Возвращает список последних сессий.
     *
     * @param limit максимальное количество
     * @return список сессий
     * @throws SessionStoreException при ошибке
     */
    List<SessionSummary> listRecent(int limit) throws SessionStoreException;

    /**
     * Удаляет все архивированные сессии старше указанного возраста.
     *
     * @param maxAgeDays максимальный возраст в днях
     * @return количество удаленных сессий
     * @throws SessionStoreException при ошибке
     */
    int purgeOldSessions(int maxAgeDays) throws SessionStoreException;

    /**
     * Краткая информация о сессии (для списков).
     */
    class SessionSummary {
        private final String id;
        private final String title;
        private final String projectName;
        private final Session.SessionStatus status;
        private final java.time.Instant createdAt;
        private final java.time.Instant updatedAt;
        private final int messageCount;

        public SessionSummary(String id, String title, String projectName,
                              Session.SessionStatus status,
                              java.time.Instant createdAt, java.time.Instant updatedAt,
                              int messageCount) {
            this.id = id;
            this.title = title;
            this.projectName = projectName;
            this.status = status;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
            this.messageCount = messageCount;
        }

        public String getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public String getProjectName() {
            return projectName;
        }

        public Session.SessionStatus getStatus() {
            return status;
        }

        public java.time.Instant getCreatedAt() {
            return createdAt;
        }

        public java.time.Instant getUpdatedAt() {
            return updatedAt;
        }

        public int getMessageCount() {
            return messageCount;
        }

        /**
         * Создает SessionSummary из полной Session.
         */
        public static SessionSummary from(Session session) {
            return new SessionSummary(
                    session.getId(),
                    session.getTitle(),
                    session.getProjectName(),
                    session.getStatus(),
                    session.getCreatedAt(),
                    session.getUpdatedAt(),
                    session.getMessageCount()
            );
        }

        @Override
        public String toString() {
            return "SessionSummary{" +
                    "id='" + id + '\'' +
                    ", title='" + title + '\'' +
                    ", project='" + projectName + '\'' +
                    ", messages=" + messageCount +
                    '}';
        }
    }

    /**
     * Исключение при ошибках работы с хранилищем.
     */
    class SessionStoreException extends Exception {
        private static final long serialVersionUID = 1L;

        public SessionStoreException(String message) {
            super(message);
        }

        public SessionStoreException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
