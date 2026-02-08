/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
 */
package com.codepilot1c.core.agent.prompts;

/**
 * Инструкции для работы с Context7 MCP сервером.
 *
 * <p>Context7 - это сервис документации, который может искать по различным
 * библиотекам. Для корректной работы с 1С:Предприятие необходимо явно
 * указывать контекст поиска.</p>
 *
 * <p>Эти инструкции добавляются в системный промпт агента для обеспечения
 * получения релевантной документации по языку BSL.</p>
 */
public final class Context7Instructions {

    private Context7Instructions() {
        // Utility class
    }

    /**
     * Идентификаторы библиотек 1С для Context7.
     * Получены через resolve-library-id.
     */
    public static final String BSL_LIBRARY_1C_DN = "/websites/1c-dn_library";
    public static final String BSL_LIBRARY_LSP = "/1c-syntax/bsl-language-server";
    public static final String BSL_LIBRARY_SSL = "/1c-syntax/ssl_3_1";

    /**
     * Основная библиотека для документации.
     */
    public static final String BSL_LIBRARY_ID = BSL_LIBRARY_1C_DN;

    /**
     * Альтернативные запросы для resolve_library_id.
     */
    public static final String[] BSL_LIBRARY_QUERIES = {
        "1c enterprise bsl",
        "1С Предприятие",
        "1c platform"
    };

    /**
     * УСИЛЕННЫЕ инструкции по работе с Context7 для получения документации 1С.
     *
     * <p>Ключевые изменения:</p>
     * <ul>
     *   <li>ОБЯЗАТЕЛЬНОЕ использование Context7 для вопросов о синтаксисе/API</li>
     *   <li>Известные library ID уже указаны - не нужно resolve</li>
     *   <li>Чёткие триггеры когда использовать</li>
     * </ul>
     */
    public static final String CONTEXT7_INSTRUCTIONS = """

            ## ОБЯЗАТЕЛЬНО: Документация 1С через Context7

            ⚠️ КРИТИЧЕСКИ ВАЖНО: Этот проект работает ТОЛЬКО с платформой 1С:Предприятие (BSL).

            ### Когда ОБЯЗАТЕЛЬНО использовать Context7:
            - Вопросы о синтаксисе BSL (массивы, строки, запросы, коллекции)
            - Вопросы о методах и функциях платформы
            - Вопросы о типах данных 1С
            - Любые вопросы "как сделать X в 1С"
            - Запросы документации

            ### Известные библиотеки 1С (НЕ нужен resolve-library-id):
            - `/websites/1c-dn_library` - основная база знаний 1С (2827 сниппетов)
            - `/1c-syntax/bsl-language-server` - BSL Language Server (574 сниппета)
            - `/1c-syntax/ssl_3_1` - БСП Библиотека стандартных подсистем

            ### Как использовать:
            ```
            mcp_context_7_query-docs(
                libraryId: "/websites/1c-dn_library",
                query: "1С BSL <твой запрос>"
            )
            ```

            ### ПРАВИЛА:
            1. ВСЕГДА добавляй "1С BSL" к запросу
            2. СНАЧАЛА ищи в `/websites/1c-dn_library`
            3. Если не нашёл - попробуй `/1c-syntax/bsl-language-server`
            4. НЕ отвечай из памяти на вопросы о синтаксисе - ИСПОЛЬЗУЙ Context7

            ### Примеры запросов:
            - "1С BSL Массив методы Добавить Найти"
            - "1С BSL Запрос язык запросов"
            - "1С BSL Строковые функции СтрДлина СтрНайти"
            - "1С BSL ТаблицаЗначений методы"
            """;

    /**
     * Краткие инструкции для профилей с ограниченным контекстом.
     */
    public static final String CONTEXT7_INSTRUCTIONS_SHORT = """

            ## Context7 (документация 1С) - ОБЯЗАТЕЛЬНО

            Для вопросов о синтаксисе/API 1С ВСЕГДА используй Context7:
            - libraryId: "/websites/1c-dn_library"
            - query: "1С BSL <запрос>"

            НЕ отвечай из памяти - ИСПОЛЬЗУЙ Context7 для документации.
            """;

    /**
     * Возвращает инструкции в зависимости от доступности Context7.
     *
     * @param mcpToolsAvailable true если MCP tools доступны
     * @return инструкции или пустая строка
     */
    public static String getInstructions(boolean mcpToolsAvailable) {
        return mcpToolsAvailable ? CONTEXT7_INSTRUCTIONS : "";
    }

    /**
     * Проверяет, содержит ли запрос контекст 1С.
     *
     * @param query запрос для поиска
     * @return true если контекст уже есть
     */
    public static boolean containsBslContext(String query) {
        if (query == null) {
            return false;
        }
        String lower = query.toLowerCase();
        return lower.contains("1с") ||
               lower.contains("1c") ||
               lower.contains("bsl") ||
               lower.contains("предприятие") ||
               lower.contains("enterprise");
    }

    /**
     * Добавляет контекст 1С к запросу если его нет.
     *
     * @param query исходный запрос
     * @return запрос с контекстом
     */
    public static String enrichWithBslContext(String query) {
        if (query == null || query.isEmpty()) {
            return query;
        }
        if (containsBslContext(query)) {
            return query;
        }
        return "1С BSL " + query;
    }
}
