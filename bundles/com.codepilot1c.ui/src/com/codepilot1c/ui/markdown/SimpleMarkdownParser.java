/*
 * Copyright (c) 2024 Example
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.codepilot1c.ui.markdown;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Простой парсер Markdown в HTML без внешних зависимостей.
 *
 * Поддерживает:
 * - Заголовки (# ## ###)
 * - Жирный и курсив (**bold** *italic*)
 * - Inline код (`code`)
 * - Блоки кода (```lang)
 * - Таблицы (| col | col |)
 * - Списки (- item, 1. item)
 * - Ссылки [text](url)
 * - Горизонтальные линии (---)
 *
 * @deprecated Используйте {@link FlexmarkParser} вместо этого класса.
 *             Этот класс имеет проблемы с плейсхолдерами для блоков кода,
 *             которые могут быть модифицированы markdown-обработкой.
 */
@Deprecated
public class SimpleMarkdownParser {

    // Patterns
    // Pattern for code blocks: handles various formats including inline and multi-line
    // Matches: ```lang\ncode```, ```lang code```, ```\ncode```, ```code```
    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile(
            "```(\\w*)[\\t ]*\\n?([\\s\\S]*?)```", Pattern.MULTILINE);

    private static final Pattern TABLE_PATTERN = Pattern.compile(
            "(^\\|.+\\|\\s*\\r?$\\n)(^\\|[-:|\\s]+\\|\\s*\\r?$\\n)((?:^\\|.+\\|\\s*\\r?$\\n?)+)",
            Pattern.MULTILINE);

    private static final Pattern HEADER_PATTERN = Pattern.compile(
            "^(#{1,6})\\s+(.+)$", Pattern.MULTILINE);

    private static final Pattern BOLD_PATTERN = Pattern.compile(
            "\\*\\*(.+?)\\*\\*|__(.+?)__");

    private static final Pattern ITALIC_PATTERN = Pattern.compile(
            "(?<![\\*_])\\*([^\\*]+?)\\*(?![\\*_])|(?<![\\*_])_([^_]+?)_(?![\\*_])");

    private static final Pattern INLINE_CODE_PATTERN = Pattern.compile(
            "`([^`]+)`");

    private static final Pattern LINK_PATTERN = Pattern.compile(
            "\\[([^\\]]+)\\]\\(([^)]+)\\)");

    private static final Pattern UNORDERED_LIST_PATTERN = Pattern.compile(
            "^[\\s]*[-*+]\\s+(.+)$", Pattern.MULTILINE);

    private static final Pattern ORDERED_LIST_PATTERN = Pattern.compile(
            "^[\\s]*\\d+\\.\\s+(.+)$", Pattern.MULTILINE);

    private static final Pattern HR_PATTERN = Pattern.compile(
            "^[-*_]{3,}\\s*$", Pattern.MULTILINE);

    private static final Pattern BLOCKQUOTE_PATTERN = Pattern.compile(
            "^>\\s*(.+)$", Pattern.MULTILINE);

    // Placeholder for code blocks during processing
    // IMPORTANT:
    // - Must NOT contain < > & " ' (escaped by escapeHtml)
    // - Must NOT contain _ or * (interpreted as italic/bold by markdown)
    // Using format with digits that won't appear in normal text
    private static final String PLACEHOLDER_PREFIX = "CODEBLOCKSTART"; //$NON-NLS-1$
    private static final String PLACEHOLDER_SUFFIX = "CODEBLOCKEND"; //$NON-NLS-1$
    private final List<String> codeBlocks = new ArrayList<>();

    /**
     * Конвертирует Markdown в HTML.
     *
     * @param markdown исходный текст в формате Markdown
     * @return HTML-разметка
     */
    public String toHtml(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return ""; //$NON-NLS-1$
        }

        codeBlocks.clear();
        // Normalize line endings to \n
        String html = markdown.replace("\r\n", "\n").replace("\r", "\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

        // Экранирование HTML-спецсимволов (кроме тех, что в code blocks)
        html = extractCodeBlocks(html);
        html = escapeHtml(html);

        // Обработка элементов Markdown
        html = processTables(html);
        html = processHeaders(html);
        html = processBlockquotes(html);
        html = processHorizontalRules(html);
        html = processLists(html);
        html = processBold(html);
        html = processItalic(html);
        html = processInlineCode(html);
        html = processLinks(html);
        html = processParagraphs(html);

        // Восстановление code blocks
        html = restoreCodeBlocks(html);

        return html;
    }

    /**
     * Извлекает блоки кода и заменяет их плейсхолдерами.
     */
    private String extractCodeBlocks(String text) {
        Matcher matcher = CODE_BLOCK_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String language = matcher.group(1);
            String code = matcher.group(2);

            // Сохраняем блок кода
            int index = codeBlocks.size();
            String codeHtml = buildCodeBlockHtml(language, code);
            codeBlocks.add(codeHtml);

            // Use unique placeholder format
            String placeholder = PLACEHOLDER_PREFIX + index + PLACEHOLDER_SUFFIX;
            matcher.appendReplacement(sb, Matcher.quoteReplacement(placeholder));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * Восстанавливает блоки кода из плейсхолдеров.
     */
    private String restoreCodeBlocks(String text) {
        String result = text;
        for (int i = 0; i < codeBlocks.size(); i++) {
            // Use unique placeholder format
            String placeholder = PLACEHOLDER_PREFIX + i + PLACEHOLDER_SUFFIX;
            result = result.replace(placeholder, codeBlocks.get(i));

            // Also handle legacy formats for compatibility (in case LLM outputs these)
            result = result.replace("%%CODEBLOCK_" + i + "%%", codeBlocks.get(i)); //$NON-NLS-1$ //$NON-NLS-2$
            result = result.replace("%%CODEBLOCK" + i + "%%", codeBlocks.get(i)); //$NON-NLS-1$ //$NON-NLS-2$
        }

        // Clean up any remaining placeholder patterns that weren't replaced
        result = cleanupUnresolvedPlaceholders(result);

        return result;
    }

    /**
     * Очищает нераспознанные плейсхолдеры.
     */
    private String cleanupUnresolvedPlaceholders(String text) {
        // Remove any remaining current format placeholders
        return text.replaceAll(Pattern.quote(PLACEHOLDER_PREFIX) + "\\d+" + Pattern.quote(PLACEHOLDER_SUFFIX), ""); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * Создает HTML для блока кода.
     */
    private String buildCodeBlockHtml(String language, String code) {
        String escapedCode = escapeHtml(code.trim());
        String langClass = (language != null && !language.isEmpty())
                ? " class=\"language-" + language + "\"" //$NON-NLS-1$ //$NON-NLS-2$
                : ""; //$NON-NLS-1$
        String langAttr = (language != null && !language.isEmpty())
                ? " data-language=\"" + escapeHtml(language) + "\"" //$NON-NLS-1$ //$NON-NLS-2$
                : ""; //$NON-NLS-1$

        return "<div class=\"code-block\"" + langAttr + ">" + //$NON-NLS-1$ //$NON-NLS-2$
               "<div class=\"code-header\">" + //$NON-NLS-1$
               (language != null && !language.isEmpty() ? "<span class=\"code-lang\">" + escapeHtml(language) + "</span>" : "") + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
               "<button class=\"copy-btn\" onclick=\"copyCode(this)\">Копировать</button>" + //$NON-NLS-1$
               "</div>" + //$NON-NLS-1$
               "<pre><code" + langClass + ">" + escapedCode + "</code></pre>" + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
               "</div>"; //$NON-NLS-1$
    }

    /**
     * Экранирует HTML-спецсимволы.
     */
    private String escapeHtml(String text) {
        return text
                .replace("&", "&amp;") //$NON-NLS-1$ //$NON-NLS-2$
                .replace("<", "&lt;") //$NON-NLS-1$ //$NON-NLS-2$
                .replace(">", "&gt;") //$NON-NLS-1$ //$NON-NLS-2$
                .replace("\"", "&quot;") //$NON-NLS-1$ //$NON-NLS-2$
                .replace("'", "&#39;"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * Обрабатывает таблицы.
     */
    private String processTables(String text) {
        Matcher matcher = TABLE_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String headerRow = matcher.group(1).trim();
            String alignRow = matcher.group(2).trim();
            String dataRows = matcher.group(3);

            StringBuilder table = new StringBuilder();
            table.append("<table>\n<thead>\n<tr>\n"); //$NON-NLS-1$

            // Parse alignments
            String[] alignCells = alignRow.split("\\|"); //$NON-NLS-1$
            List<String> alignments = new ArrayList<>();
            for (String cell : alignCells) {
                cell = cell.trim();
                if (cell.isEmpty()) continue;
                if (cell.startsWith(":") && cell.endsWith(":")) { //$NON-NLS-1$ //$NON-NLS-2$
                    alignments.add("center"); //$NON-NLS-1$
                } else if (cell.endsWith(":")) { //$NON-NLS-1$
                    alignments.add("right"); //$NON-NLS-1$
                } else {
                    alignments.add("left"); //$NON-NLS-1$
                }
            }

            // Header cells
            String[] headerCells = headerRow.split("\\|"); //$NON-NLS-1$
            int colIndex = 0;
            for (String cell : headerCells) {
                cell = cell.trim();
                if (cell.isEmpty()) continue;
                String align = colIndex < alignments.size() ? alignments.get(colIndex) : "left"; //$NON-NLS-1$
                table.append("<th style=\"text-align:").append(align).append("\">") //$NON-NLS-1$ //$NON-NLS-2$
                     .append(cell).append("</th>\n"); //$NON-NLS-1$
                colIndex++;
            }

            table.append("</tr>\n</thead>\n<tbody>\n"); //$NON-NLS-1$

            // Data rows
            String[] rows = dataRows.split("\\n"); //$NON-NLS-1$
            for (String row : rows) {
                row = row.trim();
                if (row.isEmpty() || !row.startsWith("|")) continue; //$NON-NLS-1$

                table.append("<tr>\n"); //$NON-NLS-1$
                String[] cells = row.split("\\|"); //$NON-NLS-1$
                colIndex = 0;
                for (String cell : cells) {
                    cell = cell.trim();
                    if (cell.isEmpty()) continue;
                    String align = colIndex < alignments.size() ? alignments.get(colIndex) : "left"; //$NON-NLS-1$
                    table.append("<td style=\"text-align:").append(align).append("\">") //$NON-NLS-1$ //$NON-NLS-2$
                         .append(cell).append("</td>\n"); //$NON-NLS-1$
                    colIndex++;
                }
                table.append("</tr>\n"); //$NON-NLS-1$
            }

            table.append("</tbody>\n</table>"); //$NON-NLS-1$

            matcher.appendReplacement(sb, Matcher.quoteReplacement(table.toString()));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * Обрабатывает заголовки.
     */
    private String processHeaders(String text) {
        Matcher matcher = HEADER_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            int level = matcher.group(1).length();
            String content = matcher.group(2);
            matcher.appendReplacement(sb, "<h" + level + ">" + content + "</h" + level + ">"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * Обрабатывает цитаты.
     */
    private String processBlockquotes(String text) {
        Matcher matcher = BLOCKQUOTE_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String content = matcher.group(1);
            matcher.appendReplacement(sb, "<blockquote>" + content + "</blockquote>"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * Обрабатывает горизонтальные линии.
     */
    private String processHorizontalRules(String text) {
        return HR_PATTERN.matcher(text).replaceAll("<hr>"); //$NON-NLS-1$
    }

    /**
     * Обрабатывает списки.
     */
    private String processLists(String text) {
        // Обработка неупорядоченных списков
        String[] lines = text.split("\\n"); //$NON-NLS-1$
        StringBuilder result = new StringBuilder();
        boolean inUl = false;
        boolean inOl = false;

        for (String line : lines) {
            Matcher ulMatcher = UNORDERED_LIST_PATTERN.matcher(line);
            Matcher olMatcher = ORDERED_LIST_PATTERN.matcher(line);

            if (ulMatcher.matches()) {
                if (inOl) {
                    result.append("</ol>\n"); //$NON-NLS-1$
                    inOl = false;
                }
                if (!inUl) {
                    result.append("<ul>\n"); //$NON-NLS-1$
                    inUl = true;
                }
                result.append("<li>").append(ulMatcher.group(1)).append("</li>\n"); //$NON-NLS-1$ //$NON-NLS-2$
            } else if (olMatcher.matches()) {
                if (inUl) {
                    result.append("</ul>\n"); //$NON-NLS-1$
                    inUl = false;
                }
                if (!inOl) {
                    result.append("<ol>\n"); //$NON-NLS-1$
                    inOl = true;
                }
                result.append("<li>").append(olMatcher.group(1)).append("</li>\n"); //$NON-NLS-1$ //$NON-NLS-2$
            } else {
                if (inUl) {
                    result.append("</ul>\n"); //$NON-NLS-1$
                    inUl = false;
                }
                if (inOl) {
                    result.append("</ol>\n"); //$NON-NLS-1$
                    inOl = false;
                }
                result.append(line).append("\n"); //$NON-NLS-1$
            }
        }

        if (inUl) {
            result.append("</ul>\n"); //$NON-NLS-1$
        }
        if (inOl) {
            result.append("</ol>\n"); //$NON-NLS-1$
        }

        return result.toString();
    }

    /**
     * Обрабатывает жирный текст.
     */
    private String processBold(String text) {
        Matcher matcher = BOLD_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String content = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            matcher.appendReplacement(sb, "<strong>" + Matcher.quoteReplacement(content) + "</strong>"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * Обрабатывает курсив.
     */
    private String processItalic(String text) {
        Matcher matcher = ITALIC_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String content = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            matcher.appendReplacement(sb, "<em>" + Matcher.quoteReplacement(content) + "</em>"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * Обрабатывает inline код.
     */
    private String processInlineCode(String text) {
        Matcher matcher = INLINE_CODE_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String code = matcher.group(1);
            matcher.appendReplacement(sb, "<code class=\"inline\">" + Matcher.quoteReplacement(code) + "</code>"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * Обрабатывает ссылки.
     */
    private String processLinks(String text) {
        Matcher matcher = LINK_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String linkText = matcher.group(1);
            String url = matcher.group(2);
            matcher.appendReplacement(sb,
                    "<a href=\"" + Matcher.quoteReplacement(url) + "\" target=\"_blank\">" + //$NON-NLS-1$ //$NON-NLS-2$
                    Matcher.quoteReplacement(linkText) + "</a>"); //$NON-NLS-1$
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * Обрабатывает параграфы.
     */
    private String processParagraphs(String text) {
        String[] lines = text.split("\\n\\n+"); //$NON-NLS-1$
        StringBuilder result = new StringBuilder();

        for (String paragraph : lines) {
            paragraph = paragraph.trim();
            if (paragraph.isEmpty()) continue;

            // Не оборачивать в <p> элементы, которые уже являются блочными или содержат блоки кода
            boolean hasCodeBlock = paragraph.contains(PLACEHOLDER_PREFIX) ||
                    paragraph.contains("%%CODEBLOCK_") || paragraph.contains("%%CODEBLOCK"); //$NON-NLS-1$ //$NON-NLS-2$
            if (paragraph.startsWith("<h") || //$NON-NLS-1$
                paragraph.startsWith("<table") || //$NON-NLS-1$
                paragraph.startsWith("<ul") || //$NON-NLS-1$
                paragraph.startsWith("<ol") || //$NON-NLS-1$
                paragraph.startsWith("<blockquote") || //$NON-NLS-1$
                paragraph.startsWith("<hr") || //$NON-NLS-1$
                paragraph.startsWith("<div") || //$NON-NLS-1$
                hasCodeBlock) {
                // Если параграф содержит блок кода, обработать смешанный контент
                if (hasCodeBlock) {
                    // Разбить на части: текст и блоки кода
                    paragraph = processCodeBlockParagraph(paragraph);
                }
                result.append(paragraph).append("\n"); //$NON-NLS-1$
            } else {
                // Заменить одиночные переносы строк на <br>
                paragraph = paragraph.replace("\n", "<br>\n"); //$NON-NLS-1$ //$NON-NLS-2$
                result.append("<p>").append(paragraph).append("</p>\n"); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }

        return result.toString();
    }

    /**
     * Обрабатывает параграф, содержащий блоки кода вместе с текстом.
     */
    private String processCodeBlockParagraph(String paragraph) {
        StringBuilder result = new StringBuilder();
        // Match new format (with null chars), and legacy formats (with and without underscore)
        // Using pattern that matches any of: \u0000\u0001CB#\u0001\u0000 or %%CODEBLOCK_#%% or %%CODEBLOCK#%%
        String patternStr = Pattern.quote(PLACEHOLDER_PREFIX) + "\\d+" + Pattern.quote(PLACEHOLDER_SUFFIX) +
                "|%%CODEBLOCK_?\\d+%%"; //$NON-NLS-1$
        java.util.regex.Matcher matcher = Pattern.compile(patternStr).matcher(paragraph);

        int lastEnd = 0;

        while (matcher.find()) {
            // Добавить текст перед блоком кода
            String textBefore = paragraph.substring(lastEnd, matcher.start()).trim();
            if (!textBefore.isEmpty()) {
                textBefore = textBefore.replace("\n", "<br>\n"); //$NON-NLS-1$ //$NON-NLS-2$
                result.append("<p>").append(textBefore).append("</p>\n"); //$NON-NLS-1$ //$NON-NLS-2$
            }
            // Добавить блок кода
            result.append(matcher.group()).append("\n"); //$NON-NLS-1$
            lastEnd = matcher.end();
        }

        // Добавить текст после последнего блока кода
        if (lastEnd < paragraph.length()) {
            String textAfter = paragraph.substring(lastEnd).trim();
            if (!textAfter.isEmpty()) {
                textAfter = textAfter.replace("\n", "<br>\n"); //$NON-NLS-1$ //$NON-NLS-2$
                result.append("<p>").append(textAfter).append("</p>\n"); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }

        return result.toString().trim();
    }
}
