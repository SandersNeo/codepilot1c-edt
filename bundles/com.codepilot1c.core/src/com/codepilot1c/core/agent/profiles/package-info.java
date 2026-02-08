/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
 */

/**
 * Профили агентов с разными уровнями доступа.
 *
 * <h2>Доступные профили</h2>
 * <table border="1">
 *   <tr><th>Профиль</th><th>Описание</th><th>Инструменты</th></tr>
 *   <tr>
 *     <td><b>build</b></td>
 *     <td>Полный доступ для разработки</td>
 *     <td>read, edit, write, glob, grep, shell</td>
 *   </tr>
 *   <tr>
 *     <td><b>plan</b></td>
 *     <td>Только чтение для планирования</td>
 *     <td>read, glob, grep, search</td>
 *   </tr>
 *   <tr>
 *     <td><b>explore</b></td>
 *     <td>Быстрый поиск по коду</td>
 *     <td>read, glob, grep, search</td>
 *   </tr>
 * </table>
 *
 * <h2>Использование</h2>
 * <pre>
 * // Получить профиль
 * AgentProfile profile = AgentProfileRegistry.getInstance().getBuildProfile();
 *
 * // Создать конфигурацию
 * AgentConfig config = AgentProfileRegistry.getInstance().createConfig(profile);
 *
 * // Запустить агента
 * AgentRunner runner = new AgentRunner(provider, toolRegistry);
 * runner.run("Задача", config);
 * </pre>
 *
 * @see com.codepilot1c.core.agent.AgentRunner
 * @see com.codepilot1c.core.agent.AgentConfig
 */
package com.codepilot1c.core.agent.profiles;
