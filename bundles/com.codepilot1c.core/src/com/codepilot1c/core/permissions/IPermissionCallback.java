/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
 */
package com.codepilot1c.core.permissions;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Callback для запроса подтверждения у пользователя.
 *
 * <p>Используется когда правило возвращает {@link PermissionDecision#ASK}.
 * UI реализует этот интерфейс для показа диалога подтверждения.</p>
 */
@FunctionalInterface
public interface IPermissionCallback {

    /**
     * Запрашивает подтверждение у пользователя.
     *
     * @param request детали запроса
     * @return future с ответом пользователя
     */
    CompletableFuture<PermissionResponse> requestPermission(PermissionRequest request);

    /**
     * Запрос на подтверждение разрешения.
     */
    class PermissionRequest {
        private final String toolName;
        private final String toolDescription;
        private final String action;
        private final String resource;
        private final Map<String, Object> context;
        private final boolean isDestructive;

        public PermissionRequest(String toolName, String toolDescription,
                                 String action, String resource,
                                 Map<String, Object> context, boolean isDestructive) {
            this.toolName = toolName;
            this.toolDescription = toolDescription;
            this.action = action;
            this.resource = resource;
            this.context = context;
            this.isDestructive = isDestructive;
        }

        public String getToolName() {
            return toolName;
        }

        public String getToolDescription() {
            return toolDescription;
        }

        public String getAction() {
            return action;
        }

        public String getResource() {
            return resource;
        }

        public Map<String, Object> getContext() {
            return context;
        }

        public boolean isDestructive() {
            return isDestructive;
        }

        @Override
        public String toString() {
            return "PermissionRequest{" +
                    "tool='" + toolName + '\'' +
                    ", action='" + action + '\'' +
                    ", resource='" + resource + '\'' +
                    ", destructive=" + isDestructive +
                    '}';
        }
    }

    /**
     * Ответ пользователя на запрос разрешения.
     */
    class PermissionResponse {
        private final PermissionDecision decision;
        private final boolean rememberForSession;
        private final boolean rememberPermanently;

        private PermissionResponse(PermissionDecision decision,
                                   boolean rememberForSession,
                                   boolean rememberPermanently) {
            this.decision = decision;
            this.rememberForSession = rememberForSession;
            this.rememberPermanently = rememberPermanently;
        }

        /**
         * Решение пользователя.
         */
        public PermissionDecision getDecision() {
            return decision;
        }

        /**
         * Запомнить на текущую сессию.
         */
        public boolean isRememberForSession() {
            return rememberForSession;
        }

        /**
         * Запомнить постоянно.
         */
        public boolean isRememberPermanently() {
            return rememberPermanently;
        }

        /**
         * Создает ответ "разрешить".
         */
        public static PermissionResponse allow() {
            return new PermissionResponse(PermissionDecision.ALLOW, false, false);
        }

        /**
         * Создает ответ "разрешить и запомнить на сессию".
         */
        public static PermissionResponse allowForSession() {
            return new PermissionResponse(PermissionDecision.ALLOW, true, false);
        }

        /**
         * Создает ответ "разрешить и запомнить постоянно".
         */
        public static PermissionResponse allowPermanently() {
            return new PermissionResponse(PermissionDecision.ALLOW, false, true);
        }

        /**
         * Создает ответ "запретить".
         */
        public static PermissionResponse deny() {
            return new PermissionResponse(PermissionDecision.DENY, false, false);
        }

        /**
         * Создает ответ "запретить на сессию".
         */
        public static PermissionResponse denyForSession() {
            return new PermissionResponse(PermissionDecision.DENY, true, false);
        }

        /**
         * Создает ответ "запретить постоянно".
         */
        public static PermissionResponse denyPermanently() {
            return new PermissionResponse(PermissionDecision.DENY, false, true);
        }

        @Override
        public String toString() {
            return "PermissionResponse{" +
                    "decision=" + decision +
                    ", rememberSession=" + rememberForSession +
                    ", rememberPermanent=" + rememberPermanently +
                    '}';
        }
    }
}
