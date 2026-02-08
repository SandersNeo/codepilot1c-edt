/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
 */
package com.codepilot1c.core.context7;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.codepilot1c.core.logging.VibeLogger;
import com.codepilot1c.core.provider.ILlmProvider;
import com.codepilot1c.core.tools.ITool;
import com.codepilot1c.core.tools.ToolRegistry;

/**
 * Сервис для автоматической интеграции документации 1С через Context7 MCP.
 *
 * <p>Особенности:</p>
 * <ul>
 *   <li>On-demand загрузка документации (без preload)</li>
 *   <li>Умный выбор библиотек по категории запроса</li>
 *   <li>Использование параметров topic и tokens Context7 API</li>
 *   <li>Кэширование с TTL</li>
 * </ul>
 *
 * @see <a href="https://github.com/upstash/context7">Context7 MCP</a>
 */
public class Context7DocumentationService {

    private static final VibeLogger.CategoryLogger LOG =
            VibeLogger.forClass(Context7DocumentationService.class);

    /** Singleton instance */
    private static Context7DocumentationService instance;

    // ===== Библиотеки 1С в Context7 =====

    /** База знаний 1С (основная документация) */
    public static final String LIBRARY_1C_DN = "/websites/1c-dn_library";
    /** BSL Language Server (диагностики и правила) */
    public static final String LIBRARY_BSL_LSP = "/1c-syntax/bsl-language-server";
    /** БСП 3.1 (Библиотека стандартных подсистем) */
    public static final String LIBRARY_SSL = "/1c-syntax/ssl_3_1";

    /** Информация о библиотеке Context7 */
    public static final List<LibraryInfo> ALL_LIBRARIES = List.of(
        new LibraryInfo(LIBRARY_1C_DN, "1C-DN", "База знаний 1С:Предприятие"),
        new LibraryInfo(LIBRARY_BSL_LSP, "BSL-LSP", "Диагностики и правила BSL"),
        new LibraryInfo(LIBRARY_SSL, "БСП 3.1", "Библиотека стандартных подсистем")
    );

    /**
     * Информация о библиотеке Context7.
     */
    public static class LibraryInfo {
        public final String id;
        public final String shortName;
        public final String description;

        public LibraryInfo(String id, String shortName, String description) {
            this.id = id;
            this.shortName = shortName;
            this.description = description;
        }
    }

    // ===== Настройки Context7 API =====

    /** Имя MCP tool для запроса документации */
    private static final String TOOL_QUERY_DOCS = "mcp_context_7_query-docs";

    /** Токенов на один запрос к библиотеке (Context7 сам управляет размером) */
    private static final int TOKENS_PER_LIBRARY = 3000;

    /** Максимум библиотек для параллельного запроса */
    private static final int MAX_PARALLEL_LIBRARIES = 2;

    // ===== Кэширование =====

    /** TTL кэша в миллисекундах (10 минут) */
    private static final long CACHE_TTL_MS = 10 * 60 * 1000;

    /** Максимум записей в кэше */
    private static final int MAX_CACHE_SIZE = 100;

    /** Кэш документации с TTL */
    private final Map<String, CacheEntry> documentationCache = new ConcurrentHashMap<>();

    /** Запись кэша с временем создания */
    private static class CacheEntry {
        final String content;
        final Instant createdAt;

        CacheEntry(String content) {
            this.content = content;
            this.createdAt = Instant.now();
        }

        boolean isExpired() {
            return Duration.between(createdAt, Instant.now()).toMillis() > CACHE_TTL_MS;
        }
    }

    /** Флаг готовности сервиса */
    private volatile boolean initialized = false;

    // ===== Категории запросов =====

    /**
     * Категория запроса для выбора библиотек.
     */
    public enum QueryCategory {
        /** Синтаксис BSL, типы данных, функции */
        SYNTAX,
        /** Диагностики, качество кода, линтинг */
        DIAGNOSTICS,
        /** БСП, стандартные подсистемы */
        SUBSYSTEMS,
        /** Общие вопросы */
        GENERAL
    }

    // ===== LLM-классификатор =====

    /** QueryClassifier instance for LLM-based query classification */
    private final QueryClassifier queryClassifier = QueryClassifier.getInstance();

    /** Таймаут для классификации запроса */
    private static final int CLASSIFICATION_TIMEOUT_SECONDS = 20;

    // ===== Конструктор и singleton =====

    private Context7DocumentationService() {
        // Private constructor for singleton
    }

    /**
     * Получает singleton instance сервиса.
     *
     * @return instance сервиса
     */
    public static synchronized Context7DocumentationService getInstance() {
        if (instance == null) {
            instance = new Context7DocumentationService();
        }
        return instance;
    }

    /**
     * Инициализирует сервис (on-demand режим, без preload).
     *
     * @return CompletableFuture завершающийся сразу
     */
    public CompletableFuture<Void> initialize() {
        if (initialized) {
            LOG.debug("Context7DocumentationService already initialized");
            return CompletableFuture.completedFuture(null);
        }

        LOG.info("Context7DocumentationService initialized (on-demand mode)");
        initialized = true;
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Проверяет, инициализирован ли сервис.
     *
     * @return true если сервис готов
     */
    public boolean isInitialized() {
        return initialized;
    }

    // ===== Основной API =====

    /**
     * Устанавливает LLM provider для классификатора.
     *
     * @param provider LLM provider
     */
    public void setLlmProvider(ILlmProvider provider) {
        queryClassifier.setLlmProvider(provider);
        LOG.info("Context7DocumentationService: LLM provider configured for query classification");
    }

    /**
     * Обогащает запрос релевантной документацией из Context7.
     *
     * <p>Алгоритм:</p>
     * <ol>
     *   <li>LLM классифицирует запрос по категории (или NONE если документация не нужна)</li>
     *   <li>Выбирает 1-2 релевантные библиотеки</li>
     *   <li>Извлекает тему для фокусировки поиска</li>
     *   <li>Запрашивает документацию параллельно</li>
     *   <li>Кэширует результат</li>
     * </ol>
     *
     * @param userMessage сообщение пользователя
     * @return CompletableFuture с документацией или пустой строкой
     */
    public CompletableFuture<String> enrichWithDocumentation(String userMessage) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return CompletableFuture.completedFuture("");
        }

        // Формируем ключ кэша
        String cacheKey = buildCacheKey(userMessage);

        // Проверяем кэш документации
        CacheEntry cached = documentationCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            LOG.debug("Documentation found in cache (age: %d sec)",
                    Duration.between(cached.createdAt, Instant.now()).toSeconds());
            return CompletableFuture.completedFuture(cached.content);
        }

        // Классифицируем запрос через LLM
        return queryClassifier.classify(userMessage)
            .orTimeout(CLASSIFICATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .thenCompose(category -> {
                // LLM вернул null (NONE) - документация не нужна
                if (category == null) {
                    LOG.debug("Query classified as NONE (no documentation needed): %s",
                            truncateMessage(userMessage, 50));
                    return CompletableFuture.completedFuture("");
                }

                LOG.info("Query category: %s for: %s", category, truncateMessage(userMessage, 50));

                // Выбираем библиотеки
                List<LibraryInfo> libraries = selectLibraries(category);
                LOG.debug("Selected %d libraries: %s", libraries.size(),
                        libraries.stream().map(l -> l.shortName).toList());

                // Извлекаем тему
                String topic = extractTopic(userMessage);
                LOG.debug("Extracted topic: %s", topic);

                // Формируем запрос
                String query = buildQuery(userMessage);

                // Запрашиваем документацию параллельно
                List<CompletableFuture<LibraryResult>> futures = libraries.stream()
                    .map(lib -> queryLibrary(lib, query, topic))
                    .toList();

                return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> combineResults(futures, libraries.size(), cacheKey));
            })
            .exceptionally(e -> {
                LOG.warn("Documentation enrichment failed: %s", e.getMessage());
                return "";
            });
    }

    /**
     * Объединяет результаты запросов к библиотекам.
     */
    private String combineResults(List<CompletableFuture<LibraryResult>> futures,
                                   int totalLibraries, String cacheKey) {
        StringBuilder result = new StringBuilder();
        int successCount = 0;

        for (CompletableFuture<LibraryResult> future : futures) {
            try {
                LibraryResult libResult = future.join();
                if (libResult != null && libResult.content != null && !libResult.content.isEmpty()) {
                    if (result.length() > 0) {
                        result.append("\n\n");
                    }
                    result.append("### ").append(libResult.libraryName).append("\n\n");
                    result.append(libResult.content);
                    successCount++;
                }
            } catch (Exception e) {
                LOG.debug("Failed to get library result: %s", e.getMessage());
            }
        }

        if (result.length() > 0) {
            String formatted = formatDocumentation(result.toString());
            // Сохраняем в кэш
            putInCache(cacheKey, formatted);
            LOG.info("Documentation enrichment complete: %d chars from %d/%d libraries",
                    formatted.length(), successCount, totalLibraries);
            return formatted;
        }

        LOG.debug("No documentation found for query");
        return "";
    }

    /**
     * Обрезает сообщение для логирования.
     */
    private String truncateMessage(String message, int maxLen) {
        if (message == null) return "null";
        return message.length() > maxLen ? message.substring(0, maxLen) + "..." : message;
    }

    /**
     * Получает контекст документации для сообщения.
     *
     * @param userMessage сообщение пользователя
     * @param includeBase игнорируется (совместимость)
     * @return CompletableFuture с документацией
     */
    public CompletableFuture<String> getDocumentationContext(String userMessage, boolean includeBase) {
        return enrichWithDocumentation(userMessage);
    }

    // ===== Классификация и выбор библиотек =====

    /**
     * Классифицирует запрос по категории (синхронно).
     *
     * <p>Использует LLM-классификатор для определения категории запроса.
     * Блокирует текущий поток до получения результата.</p>
     *
     * @param userMessage сообщение пользователя
     * @return категория запроса (GENERAL если классификация не удалась)
     */
    public QueryCategory classifyQuery(String userMessage) {
        try {
            QueryCategory result = queryClassifier.classify(userMessage)
                .orTimeout(CLASSIFICATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .join();
            return result != null ? result : QueryCategory.GENERAL;
        } catch (Exception e) {
            LOG.warn("Sync classification failed, using GENERAL: %s", e.getMessage());
            return QueryCategory.GENERAL;
        }
    }

    /**
     * Классифицирует запрос по категории (асинхронно).
     *
     * <p>Использует LLM-классификатор для определения категории запроса.</p>
     *
     * @param userMessage сообщение пользователя
     * @return CompletableFuture с категорией (null если документация не нужна)
     */
    public CompletableFuture<QueryCategory> classifyQueryAsync(String userMessage) {
        return queryClassifier.classify(userMessage)
            .orTimeout(CLASSIFICATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Выбирает библиотеки для запроса по категории.
     *
     * @param category категория запроса
     * @return список библиотек (1-2)
     */
    public List<LibraryInfo> selectLibraries(QueryCategory category) {
        return switch (category) {
            case SYNTAX -> List.of(ALL_LIBRARIES.get(0)); // 1C-DN
            case DIAGNOSTICS -> List.of(ALL_LIBRARIES.get(1)); // BSL-LSP
            case SUBSYSTEMS -> List.of(ALL_LIBRARIES.get(2)); // SSL
            case GENERAL -> List.of(ALL_LIBRARIES.get(0), ALL_LIBRARIES.get(2)); // 1C-DN + SSL
        };
    }

    // ===== Извлечение темы и запроса =====

    /**
     * Извлекает тему из запроса для параметра topic Context7.
     *
     * @param userMessage сообщение пользователя
     * @return тема или null
     */
    private String extractTopic(String userMessage) {
        // Ищем конкретные объекты/концепции
        List<Pattern> topicPatterns = List.of(
            // Типы данных
            Pattern.compile("(?iu)(массив|array)"),
            Pattern.compile("(?iu)(структур[ауе]|structure)"),
            Pattern.compile("(?iu)(соответстви[ею]|map)"),
            Pattern.compile("(?iu)(таблиц.*значен|value.*table)"),
            Pattern.compile("(?iu)(строк[ауе]|string)"),
            Pattern.compile("(?iu)(числ[оа]|number)"),
            Pattern.compile("(?iu)(дат[ауе]|date)"),
            // Объекты метаданных
            Pattern.compile("(?iu)(справочник|catalog)"),
            Pattern.compile("(?iu)(документ|document)"),
            Pattern.compile("(?iu)(регистр.*сведен|information.*register)"),
            Pattern.compile("(?iu)(регистр.*накоплен|accumulation.*register)"),
            Pattern.compile("(?iu)(запрос|query)"),
            // Концепции
            Pattern.compile("(?iu)(транзакци|transaction)"),
            Pattern.compile("(?iu)(блокировк|lock)"),
            Pattern.compile("(?iu)(событи[яе]|event)"),
            Pattern.compile("(?iu)(форм[ауе]|form)")
        );

        for (Pattern pattern : topicPatterns) {
            Matcher matcher = pattern.matcher(userMessage);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }

        return null;
    }

    /**
     * Формирует поисковый запрос для Context7.
     *
     * @param userMessage сообщение пользователя
     * @return запрос
     */
    private String buildQuery(String userMessage) {
        // Убираем стоп-слова, оставляем суть
        String cleaned = userMessage
            .replaceAll("(?iu)(как|как бы|можно|нужно|хочу|помоги|подскажи|объясни|расскажи)", "")
            .replaceAll("(?iu)(использовать|работать|сделать|создать|написать)", "")
            .replaceAll("(?iu)(пожалуйста|please)", "")
            .replaceAll("\\s+", " ")
            .trim();

        // Добавляем контекст 1С
        return "1С BSL " + cleaned;
    }

    /**
     * Формирует ключ кэша.
     */
    private String buildCacheKey(String userMessage) {
        // Нормализуем для лучшего попадания в кэш
        return userMessage.toLowerCase().replaceAll("\\s+", " ").trim();
    }

    // ===== Запрос к Context7 =====

    /**
     * Результат запроса к библиотеке.
     */
    private static class LibraryResult {
        final String libraryName;
        final String content;

        LibraryResult(String libraryName, String content) {
            this.libraryName = libraryName;
            this.content = content;
        }
    }

    /**
     * Запрашивает документацию из одной библиотеки.
     *
     * @param library библиотека
     * @param query поисковый запрос
     * @param topic тема (опционально)
     * @return CompletableFuture с результатом
     */
    private CompletableFuture<LibraryResult> queryLibrary(LibraryInfo library, String query, String topic) {
        ToolRegistry registry = ToolRegistry.getInstance();
        ITool tool = registry.getTool(TOOL_QUERY_DOCS);

        if (tool == null) {
            LOG.warn("Context7 tool not found: %s. Is Context7 MCP server running?", TOOL_QUERY_DOCS);
            return CompletableFuture.completedFuture(null);
        }

        Map<String, Object> params = new HashMap<>();
        params.put("libraryId", library.id);
        params.put("query", query);
        // Context7 API: tokens parameter limits response size (default 5000, min 1000)
        // See: https://github.com/upstash/context7
        params.put("tokens", TOKENS_PER_LIBRARY);

        if (topic != null && !topic.isEmpty()) {
            // Context7 API: topic parameter focuses search on specific area
            params.put("topic", topic);
        }

        LOG.debug("Querying Context7: library=%s, query=%s, topic=%s, tokens=%d",
                library.shortName, query, topic, TOKENS_PER_LIBRARY);

        return tool.execute(params)
            .thenApply(result -> {
                if (result.isSuccess() && result.getContent() != null && !result.getContent().isEmpty()) {
                    LOG.debug("Context7 [%s] returned %d chars",
                            library.shortName, result.getContent().length());
                    return new LibraryResult(library.description, result.getContent());
                } else {
                    LOG.debug("Context7 [%s] returned empty or failed: %s",
                            library.shortName, result.getErrorMessage());
                    return null;
                }
            })
            .exceptionally(e -> {
                LOG.warn("Context7 [%s] query exception: %s", library.shortName, e.getMessage());
                return null;
            });
    }

    // ===== Форматирование =====

    /**
     * Форматирует документацию для включения в контекст.
     *
     * @param rawDoc сырая документация
     * @return отформатированная документация
     */
    private String formatDocumentation(String rawDoc) {
        return "\n---\n## Справочная информация из документации 1С\n\n" + rawDoc + "\n---\n";
    }

    // ===== Кэширование =====

    /**
     * Сохраняет в кэш с проверкой размера.
     */
    private void putInCache(String key, String value) {
        // Очищаем устаревшие записи если кэш переполнен
        if (documentationCache.size() >= MAX_CACHE_SIZE) {
            cleanExpiredCache();
        }
        documentationCache.put(key, new CacheEntry(value));
    }

    /**
     * Очищает устаревшие записи кэша (thread-safe implementation).
     */
    private void cleanExpiredCache() {
        int removed = 0;
        for (var entry : documentationCache.entrySet()) {
            if (entry.getValue().isExpired()) {
                // Atomic remove only if value hasn't changed
                if (documentationCache.remove(entry.getKey(), entry.getValue())) {
                    removed++;
                }
            }
        }
        if (removed > 0) {
            LOG.debug("Cleaned %d expired cache entries", removed);
        }
    }

    /**
     * Очищает весь кэш документации.
     */
    public void clearCache() {
        documentationCache.clear();
        LOG.info("Documentation cache cleared");
    }

    /**
     * Сбрасывает сервис для повторной инициализации.
     */
    public void reset() {
        documentationCache.clear();
        initialized = false;
        LOG.info("Context7DocumentationService reset");
    }

    // ===== Для совместимости =====

    /**
     * @deprecated Не используется в on-demand режиме
     */
    @Deprecated
    public String getBaseDocumentation() {
        return "";
    }
}
