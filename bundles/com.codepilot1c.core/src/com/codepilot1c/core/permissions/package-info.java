/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
 */

/**
 * Система разрешений для инструментов агента.
 *
 * <h2>Основные компоненты</h2>
 * <ul>
 *   <li>{@link com.codepilot1c.core.permissions.PermissionDecision} - ALLOW/DENY/ASK</li>
 *   <li>{@link com.codepilot1c.core.permissions.PermissionRule} - правило разрешения</li>
 *   <li>{@link com.codepilot1c.core.permissions.IPermissionManager} - интерфейс менеджера</li>
 *   <li>{@link com.codepilot1c.core.permissions.PermissionManager} - реализация</li>
 *   <li>{@link com.codepilot1c.core.permissions.IPermissionCallback} - callback для UI</li>
 * </ul>
 *
 * <h2>Использование</h2>
 * <pre>
 * // Получить менеджер
 * IPermissionManager pm = PermissionManager.getInstance();
 *
 * // Проверить разрешение
 * pm.check("shell", "execute", Map.of("command", "git status"))
 *     .thenAccept(decision -> {
 *         if (decision.isAllowed()) {
 *             // Выполнить
 *         }
 *     });
 *
 * // Добавить правило
 * pm.addRule(PermissionRule.allow("read_file").forAllResources());
 * pm.addRule(PermissionRule.deny("shell").forResourcePattern("rm -rf *"));
 * pm.addRule(PermissionRule.ask("edit_file").forResourcePattern("*.java"));
 * </pre>
 *
 * <h2>Правила по умолчанию</h2>
 * <ul>
 *   <li>read_file, glob, grep - ALLOW</li>
 *   <li>edit_file, write_file - ASK</li>
 *   <li>shell: опасные команды - DENY, git/mvn - ALLOW, остальные - ASK</li>
 * </ul>
 *
 * @see com.codepilot1c.core.agent.AgentRunner
 * @see com.codepilot1c.core.tools.ITool
 */
package com.codepilot1c.core.permissions;
