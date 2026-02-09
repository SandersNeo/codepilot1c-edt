/*
 * Copyright (c) 2024 Example
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.codepilot1c.ui.views;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;

import com.codepilot1c.ui.theme.ThemeManager;
import com.codepilot1c.ui.theme.VibeTheme;

/**
 * Renders Markdown-formatted text in a StyledText widget.
 *
 * <p>Supports the following Markdown elements:</p>
 * <ul>
 *   <li>Headers (# ## ###)</li>
 *   <li>Bold (**text** or __text__)</li>
 *   <li>Italic (*text* or _text_)</li>
 *   <li>Inline code (`code`)</li>
 *   <li>Code blocks (```language ... ```)</li>
 *   <li>Bullet lists (- or *)</li>
 *   <li>Numbered lists (1. 2. etc.)</li>
 *   <li>Blockquotes (> text)</li>
 * </ul>
 */
public class MarkdownRenderer {

    // Patterns for Markdown elements
    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("```(\\w*)[\\t ]*\\r?\\n([\\s\\S]*?)```", Pattern.MULTILINE); //$NON-NLS-1$
    private static final Pattern INLINE_CODE_PATTERN = Pattern.compile("`([^`]+)`"); //$NON-NLS-1$
    private static final Pattern BOLD_PATTERN = Pattern.compile("\\*\\*(.+?)\\*\\*|__(.+?)__"); //$NON-NLS-1$
    private static final Pattern ITALIC_PATTERN = Pattern.compile("(?<![\\*_])\\*([^\\*]+)\\*(?![\\*_])|(?<![\\*_])_([^_]+)_(?![\\*_])"); //$NON-NLS-1$
    private static final Pattern HEADER_PATTERN = Pattern.compile("^(#{1,6})\\s+(.+)$", Pattern.MULTILINE); //$NON-NLS-1$
    private static final Pattern BULLET_PATTERN = Pattern.compile("^\\s*[-*]\\s+(.+)$", Pattern.MULTILINE); //$NON-NLS-1$
    private static final Pattern NUMBERED_PATTERN = Pattern.compile("^\\s*(\\d+)\\.\\s+(.+)$", Pattern.MULTILINE); //$NON-NLS-1$
    private static final Pattern BLOCKQUOTE_PATTERN = Pattern.compile("^>\\s*(.+)$", Pattern.MULTILINE); //$NON-NLS-1$

    // Table pattern: matches markdown tables with | separators
    private static final Pattern TABLE_PATTERN = Pattern.compile(
            "(^\\|.+\\|\\s*\\r?\\n)(^\\|[-:|\\s]+\\|\\s*\\r?\\n)((?:^\\|.+\\|\\s*\\r?\\n?)+)", //$NON-NLS-1$
            Pattern.MULTILINE);

    private final StyledText styledText;
    private final VibeTheme theme;

    // Colors from theme
    private Color inlineCodeBackground;
    private Color codeBlockBackground;
    private Color codeColor;
    private Color headerColor;
    private Color quoteColor;
    private Color tableColor;

    // Fonts from theme
    private Font boldFont;
    private Font italicFont;
    private Font codeFont;
    private Font headerFont;

    /**
     * Creates a new Markdown renderer for the given StyledText widget.
     *
     * @param styledText the widget to render into
     */
    public MarkdownRenderer(StyledText styledText) {
        this.styledText = styledText;
        this.theme = ThemeManager.getInstance().getTheme();
        initializeResources();
    }

    private void initializeResources() {
        // Get colors from theme
        inlineCodeBackground = theme.getInlineCodeBackground();
        codeBlockBackground = theme.getCodeBackground();
        codeColor = theme.getCodeText();
        headerColor = theme.getAccent();
        quoteColor = theme.getTextMuted();
        tableColor = theme.getAccent();

        // Get fonts from theme
        boldFont = theme.getFontBold();
        italicFont = theme.getFontItalic();
        codeFont = theme.getFontMono();
        headerFont = theme.getFontHeader();
    }

    /**
     * Renders Markdown text into the StyledText widget at the current position.
     *
     * @param markdown the Markdown text to render
     * @return the rendered plain text (with Markdown syntax removed)
     */
    public String render(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return ""; //$NON-NLS-1$
        }

        // Process the Markdown and collect style ranges
        List<StyleRange> styles = new ArrayList<>();
        String plainText = processMarkdown(markdown, styles);

        return plainText;
    }

    /**
     * Appends Markdown text to the StyledText widget with formatting.
     *
     * @param markdown the Markdown text to append
     * @param baseOffset the offset in the StyledText where the text will be appended
     */
    public void appendFormatted(String markdown, int baseOffset) {
        if (markdown == null || markdown.isEmpty()) {
            return;
        }

        // Process the Markdown and collect style ranges
        List<StyleRange> styles = new ArrayList<>();
        String plainText = processMarkdown(markdown, styles);

        // Adjust style ranges to the actual offset
        for (StyleRange style : styles) {
            style.start += baseOffset;
        }

        // Append text
        styledText.append(plainText);

        // Apply styles
        for (StyleRange style : styles) {
            if (style.start >= 0 && style.start + style.length <= styledText.getCharCount()) {
                styledText.setStyleRange(style);
            }
        }
    }

    /**
     * Processes Markdown text and extracts plain text with style ranges.
     *
     * @param markdown the Markdown text
     * @param styles list to collect style ranges (will be populated)
     * @return the plain text with Markdown syntax removed
     */
    private String processMarkdown(String markdown, List<StyleRange> styles) {
        StringBuilder result = new StringBuilder();
        String text = markdown;

        // First, extract and process code blocks (they should not be further processed)
        List<CodeBlock> codeBlocks = new ArrayList<>();
        Matcher codeBlockMatcher = CODE_BLOCK_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        int blockIndex = 0;
        while (codeBlockMatcher.find()) {
            String language = codeBlockMatcher.group(1);
            String code = codeBlockMatcher.group(2);
            String placeholder = "\u0000CODEBLOCK" + blockIndex + "\u0000"; //$NON-NLS-1$ //$NON-NLS-2$
            codeBlocks.add(new CodeBlock(language, code, placeholder));
            codeBlockMatcher.appendReplacement(sb, Matcher.quoteReplacement(placeholder));
            blockIndex++;
        }
        codeBlockMatcher.appendTail(sb);
        text = sb.toString();

        // Process tables (before other elements to avoid conflicts)
        text = processTables(text, styles);

        // Process headers
        text = processHeaders(text, styles, result);

        // Process blockquotes
        text = processBlockquotes(text, styles);

        // Process bold (before italic to handle ***text***)
        text = processBold(text, styles);

        // Process italic
        text = processItalic(text, styles);

        // Process inline code
        text = processInlineCode(text, styles);

        // Process bullet lists
        text = processBulletLists(text, styles);

        // Process numbered lists
        text = processNumberedLists(text, styles);

        // Restore code blocks with styling
        for (CodeBlock block : codeBlocks) {
            int placeholderPos = text.indexOf(block.placeholder);
            if (placeholderPos >= 0) {
                String codeText = block.code;
                // Remove trailing newline if present
                if (codeText.endsWith("\n")) { //$NON-NLS-1$
                    codeText = codeText.substring(0, codeText.length() - 1);
                }

                // Create style for fenced code block
                StyleRange codeStyle = new StyleRange();
                codeStyle.start = placeholderPos;
                codeStyle.length = codeText.length();
                codeStyle.font = codeFont;
                codeStyle.foreground = codeColor;
                codeStyle.background = codeBlockBackground;
                styles.add(codeStyle);

                text = text.replace(block.placeholder, codeText);
            }
        }

        return text;
    }

    private String processHeaders(String text, List<StyleRange> styles, StringBuilder resultBuilder) {
        Matcher matcher = HEADER_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            int headerLevel = matcher.group(1).length();
            String headerText = matcher.group(2);

            // Calculate position in result
            int startPos = sb.length();

            // Replace the header markdown with just the text
            matcher.appendReplacement(sb, Matcher.quoteReplacement(headerText));

            // Create style for header
            StyleRange style = new StyleRange();
            style.start = startPos;
            style.length = headerText.length();
            style.font = headerFont;
            style.foreground = headerColor;
            styles.add(style);
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    private String processBlockquotes(String text, List<StyleRange> styles) {
        Matcher matcher = BLOCKQUOTE_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String quoteText = "│ " + matcher.group(1); //$NON-NLS-1$
            int startPos = sb.length();

            matcher.appendReplacement(sb, Matcher.quoteReplacement(quoteText));

            StyleRange style = new StyleRange();
            style.start = startPos;
            style.length = quoteText.length();
            style.foreground = quoteColor;
            style.font = italicFont;
            styles.add(style);
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    private String processBold(String text, List<StyleRange> styles) {
        Matcher matcher = BOLD_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String boldText = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            int startPos = sb.length();

            matcher.appendReplacement(sb, Matcher.quoteReplacement(boldText));

            StyleRange style = new StyleRange();
            style.start = startPos;
            style.length = boldText.length();
            style.font = boldFont;
            style.fontStyle = SWT.BOLD;
            styles.add(style);
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    private String processItalic(String text, List<StyleRange> styles) {
        Matcher matcher = ITALIC_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String italicText = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            int startPos = sb.length();

            matcher.appendReplacement(sb, Matcher.quoteReplacement(italicText));

            StyleRange style = new StyleRange();
            style.start = startPos;
            style.length = italicText.length();
            style.font = italicFont;
            style.fontStyle = SWT.ITALIC;
            styles.add(style);
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    private String processInlineCode(String text, List<StyleRange> styles) {
        Matcher matcher = INLINE_CODE_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String codeText = matcher.group(1);
            int startPos = sb.length();

            matcher.appendReplacement(sb, Matcher.quoteReplacement(codeText));

            StyleRange style = new StyleRange();
            style.start = startPos;
            style.length = codeText.length();
            style.font = codeFont;
            style.foreground = codeColor;
            style.background = inlineCodeBackground;
            styles.add(style);
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    private String processBulletLists(String text, List<StyleRange> styles) {
        Matcher matcher = BULLET_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String listText = "  • " + matcher.group(1); //$NON-NLS-1$
            matcher.appendReplacement(sb, Matcher.quoteReplacement(listText));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    private String processNumberedLists(String text, List<StyleRange> styles) {
        Matcher matcher = NUMBERED_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String number = matcher.group(1);
            String listText = "  " + number + ". " + matcher.group(2); //$NON-NLS-1$ //$NON-NLS-2$
            matcher.appendReplacement(sb, Matcher.quoteReplacement(listText));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * Processes Markdown tables and converts them to ASCII art tables.
     */
    private String processTables(String text, List<StyleRange> styles) {
        Matcher matcher = TABLE_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String headerRow = matcher.group(1).trim();
            String dataRows = matcher.group(3);

            // Parse header cells
            String[] headers = parseTableRow(headerRow);
            if (headers.length == 0) {
                continue;
            }

            // Parse data rows
            String[] dataLines = dataRows.split("\\n"); //$NON-NLS-1$
            List<String[]> rows = new ArrayList<>();
            for (String line : dataLines) {
                line = line.trim();
                if (!line.isEmpty() && line.startsWith("|")) { //$NON-NLS-1$
                    String[] cells = parseTableRow(line);
                    if (cells.length > 0) {
                        rows.add(cells);
                    }
                }
            }

            // Calculate column widths
            int[] widths = new int[headers.length];
            for (int i = 0; i < headers.length; i++) {
                widths[i] = headers[i].length();
            }
            for (String[] row : rows) {
                for (int i = 0; i < Math.min(row.length, widths.length); i++) {
                    widths[i] = Math.max(widths[i], row[i].length());
                }
            }

            // Build ASCII table
            StringBuilder table = new StringBuilder();
            int startPos = sb.length();

            // Top border
            table.append(buildTableBorder(widths, '┌', '┬', '┐')).append("\n"); //$NON-NLS-1$

            // Header row
            table.append(buildTableRow(headers, widths)).append("\n"); //$NON-NLS-1$

            // Header separator
            table.append(buildTableBorder(widths, '├', '┼', '┤')).append("\n"); //$NON-NLS-1$

            // Data rows
            for (String[] row : rows) {
                table.append(buildTableRow(row, widths)).append("\n"); //$NON-NLS-1$
            }

            // Bottom border
            table.append(buildTableBorder(widths, '└', '┴', '┘')); //$NON-NLS-1$

            String tableText = table.toString();
            matcher.appendReplacement(sb, Matcher.quoteReplacement(tableText));

            // Add style for the table
            StyleRange style = new StyleRange();
            style.start = startPos;
            style.length = tableText.length();
            style.font = codeFont;
            style.foreground = tableColor;
            styles.add(style);
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * Parses a table row into cells.
     */
    private String[] parseTableRow(String row) {
        // Remove leading and trailing |
        row = row.trim();
        if (row.startsWith("|")) { //$NON-NLS-1$
            row = row.substring(1);
        }
        if (row.endsWith("|")) { //$NON-NLS-1$
            row = row.substring(0, row.length() - 1);
        }

        String[] cells = row.split("\\|"); //$NON-NLS-1$
        for (int i = 0; i < cells.length; i++) {
            cells[i] = cells[i].trim();
        }
        return cells;
    }

    /**
     * Builds a table border line.
     */
    private String buildTableBorder(int[] widths, char left, char middle, char right) {
        StringBuilder sb = new StringBuilder();
        sb.append(left);
        for (int i = 0; i < widths.length; i++) {
            for (int j = 0; j < widths[i] + 2; j++) {
                sb.append('─');
            }
            if (i < widths.length - 1) {
                sb.append(middle);
            }
        }
        sb.append(right);
        return sb.toString();
    }

    /**
     * Builds a table data row.
     */
    private String buildTableRow(String[] cells, int[] widths) {
        StringBuilder sb = new StringBuilder();
        sb.append('│');
        for (int i = 0; i < widths.length; i++) {
            sb.append(' ');
            String cell = i < cells.length ? cells[i] : ""; //$NON-NLS-1$
            sb.append(cell);
            // Pad with spaces
            for (int j = cell.length(); j < widths[i]; j++) {
                sb.append(' ');
            }
            sb.append(' ');
            sb.append('│');
        }
        return sb.toString();
    }

    /**
     * Disposes resources created by this renderer.
     * Call this when the parent widget is disposed.
     * Note: Colors and fonts are now managed by ThemeManager, so nothing to dispose here.
     */
    public void dispose() {
        // All colors and fonts are managed by ThemeManager - no local disposal needed
    }

    /**
     * Helper class to store code block information during processing.
     */
    private static class CodeBlock {
        final String language;
        final String code;
        final String placeholder;

        CodeBlock(String language, String code, String placeholder) {
            this.language = language;
            this.code = code;
            this.placeholder = placeholder;
        }
    }
}
