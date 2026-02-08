/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
 */
package com.codepilot1c.core.permissions;

/**
 * Решение системы разрешений.
 */
public enum PermissionDecision {

    /**
     * Действие разрешено.
     */
    ALLOW,

    /**
     * Действие запрещено.
     */
    DENY,

    /**
     * Требуется подтверждение пользователя.
     */
    ASK;

    /**
     * Разрешено ли действие.
     */
    public boolean isAllowed() {
        return this == ALLOW;
    }

    /**
     * Запрещено ли действие.
     */
    public boolean isDenied() {
        return this == DENY;
    }

    /**
     * Требуется ли подтверждение.
     */
    public boolean requiresConfirmation() {
        return this == ASK;
    }
}
