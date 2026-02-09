/*
 * Copyright (c) 2024 Example
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 * SPDX-License-Identifier: AGPL-3.0-only
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
