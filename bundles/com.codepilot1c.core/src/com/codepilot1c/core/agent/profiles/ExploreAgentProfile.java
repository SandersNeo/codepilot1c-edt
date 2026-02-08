/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
 */
package com.codepilot1c.core.agent.profiles;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.codepilot1c.core.agent.prompts.Context7Instructions;
import com.codepilot1c.core.agent.prompts.PromptProviderRegistry;
import com.codepilot1c.core.permissions.PermissionRule;

/**
 * Профиль агента для быстрого исследования кодовой базы.
 *
 * <p>Возможности:</p>
 * <ul>
 *   <li>Быстрый поиск файлов (glob)</li>
 *   <li>Поиск по содержимому (grep)</li>
 *   <li>Чтение файлов</li>
 *   <li>Семантический поиск (search_codebase)</li>
 * </ul>
 *
 * <p>Особенности:</p>
 * <ul>
 *   <li>Оптимизирован для скорости</li>
 *   <li>Минимум шагов (15)</li>
 *   <li>Короткий таймаут (2 мин)</li>
 *   <li>Фокус на навигации</li>
 * </ul>
 *
 * <p>Используется для:</p>
 * <ul>
 *   <li>Поиска определений</li>
 *   <li>Навигации по коду</li>
 *   <li>Ответов на вопросы о структуре</li>
 *   <li>Быстрого анализа</li>
 * </ul>
 */
public class ExploreAgentProfile implements AgentProfile {

    public static final String ID = "explore";

    private static final Set<String> ALLOWED_TOOLS = new HashSet<>(Arrays.asList(
            "read_file",
            "glob",
            "grep",
            "list_files",
            "search_codebase"
    ));

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return "Исследование";
    }

    @Override
    public String getDescription() {
        return "Быстрый поиск и навигация по кодовой базе. Оптимизирован для " +
               "скорости ответов на вопросы о структуре и содержимом кода.";
    }

    @Override
    public Set<String> getAllowedTools() {
        return ALLOWED_TOOLS;
    }

    @Override
    public List<PermissionRule> getDefaultPermissions() {
        return Arrays.asList(
                PermissionRule.allow("read_file").forAllResources(),
                PermissionRule.allow("glob").forAllResources(),
                PermissionRule.allow("grep").forAllResources(),
                PermissionRule.allow("list_files").forAllResources(),
                PermissionRule.allow("search_codebase").forAllResources()
        );
    }

    @Override
    public String getSystemPromptAddition() {
        String defaultPrompt = """
                Ты - быстрый помощник для навигации по коду 1С:Предприятие.

                Твоя задача - БЫСТРО находить нужную информацию:
                1. Используй glob для поиска файлов по паттерну
                2. Используй grep для поиска текста в файлах
                3. Используй read_file для просмотра содержимого
                4. Используй search_codebase для семантического поиска

                ВАЖНО:
                - Отвечай кратко и по существу
                - Показывай конкретные файлы и строки
                - Не углубляйся в детали без необходимости
                - Если найдено много результатов, показывай самые релевантные

                Формат ответа:
                **Найдено в:** `path/to/file.bsl:123`
                ```bsl
                // релевантный код
                ```

                Доступные инструменты: read_file, glob, grep, list_files, search_codebase.
                """ + Context7Instructions.CONTEXT7_INSTRUCTIONS_SHORT;
        return PromptProviderRegistry.getInstance().getSystemPromptAddition(getId(), defaultPrompt);
    }

    @Override
    public int getMaxSteps() {
        return 15;
    }

    @Override
    public long getTimeoutMs() {
        return 2 * 60 * 1000; // 2 minutes
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public boolean canExecuteShell() {
        return false;
    }
}
