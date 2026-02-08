/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
 */
package com.codepilot1c.core.diff;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilities for parsing AI-generated code and creating code changes.
 *
 * <p>Handles extraction of code from markdown code blocks and
 * creation of appropriate CodeChange objects.</p>
 */
public final class CodeDiffUtils {

    /** Pattern for markdown code blocks with optional language. */
    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile(
            "```(\\w*)\\s*\\n(.*?)\\n```", //$NON-NLS-1$
            Pattern.DOTALL);

    /** Pattern for inline code. */
    private static final Pattern INLINE_CODE_PATTERN = Pattern.compile("`([^`]+)`"); //$NON-NLS-1$

    private CodeDiffUtils() {
        // Utility class
    }

    /**
     * Extracts code from markdown code blocks in AI response.
     *
     * @param response the AI response text
     * @return list of extracted code blocks
     */
    public static List<CodeBlock> extractCodeBlocks(String response) {
        List<CodeBlock> blocks = new ArrayList<>();
        if (response == null || response.isEmpty()) {
            return blocks;
        }

        Matcher matcher = CODE_BLOCK_PATTERN.matcher(response);
        while (matcher.find()) {
            String language = matcher.group(1);
            String code = matcher.group(2);
            blocks.add(new CodeBlock(language, code, matcher.start(), matcher.end()));
        }

        return blocks;
    }

    /**
     * Extracts the first code block from an AI response.
     *
     * @param response the AI response text
     * @return the code content, or null if no code block found
     */
    public static String extractFirstCodeBlock(String response) {
        List<CodeBlock> blocks = extractCodeBlocks(response);
        return blocks.isEmpty() ? null : blocks.get(0).getCode();
    }

    /**
     * Extracts code blocks with a specific language.
     *
     * @param response the AI response text
     * @param language the language to filter (e.g., "bsl", "java")
     * @return list of matching code blocks
     */
    public static List<CodeBlock> extractCodeBlocksByLanguage(String response, String language) {
        List<CodeBlock> all = extractCodeBlocks(response);
        if (language == null || language.isEmpty()) {
            return all;
        }

        List<CodeBlock> filtered = new ArrayList<>();
        for (CodeBlock block : all) {
            if (language.equalsIgnoreCase(block.getLanguage())) {
                filtered.add(block);
            }
        }
        return filtered;
    }

    /**
     * Creates an insertion change from AI-generated code.
     *
     * @param offset the insertion offset
     * @param aiResponse the AI response containing code
     * @return the code change, or null if no code found
     */
    public static CodeChange createInsertionFromResponse(int offset, String aiResponse) {
        String code = extractFirstCodeBlock(aiResponse);
        if (code == null) {
            // Try to use the response directly if no code block
            code = aiResponse.trim();
        }

        if (code.isEmpty()) {
            return null;
        }

        return CodeChange.insert(offset, code)
                .description("AI-generated code insertion") //$NON-NLS-1$
                .build();
    }

    /**
     * Creates a replacement change from AI-generated code.
     *
     * @param offset the start offset
     * @param length the length to replace
     * @param oldText the original text
     * @param aiResponse the AI response containing new code
     * @return the code change, or null if no code found
     */
    public static CodeChange createReplacementFromResponse(int offset, int length,
            String oldText, String aiResponse) {

        String newCode = extractFirstCodeBlock(aiResponse);
        if (newCode == null) {
            // Try to use the response directly if no code block
            newCode = aiResponse.trim();
        }

        if (newCode.isEmpty()) {
            return null;
        }

        return CodeChange.replace(offset, length, oldText, newCode)
                .description("AI-generated code replacement") //$NON-NLS-1$
                .build();
    }

    /**
     * Strips markdown formatting from text, preserving code content.
     *
     * @param text the markdown text
     * @return plain text with code preserved
     */
    public static String stripMarkdown(String text) {
        if (text == null) {
            return ""; //$NON-NLS-1$
        }

        // Extract code blocks first
        List<CodeBlock> blocks = extractCodeBlocks(text);
        if (!blocks.isEmpty()) {
            // Return just the code if there are code blocks
            StringBuilder sb = new StringBuilder();
            for (CodeBlock block : blocks) {
                if (sb.length() > 0) {
                    sb.append("\n\n"); //$NON-NLS-1$
                }
                sb.append(block.getCode());
            }
            return sb.toString();
        }

        // Otherwise strip common markdown
        String result = text;

        // Remove headers
        result = result.replaceAll("(?m)^#{1,6}\\s+", ""); //$NON-NLS-1$ //$NON-NLS-2$

        // Remove bold/italic
        result = result.replaceAll("\\*\\*(.+?)\\*\\*", "$1"); //$NON-NLS-1$ //$NON-NLS-2$
        result = result.replaceAll("\\*(.+?)\\*", "$1"); //$NON-NLS-1$ //$NON-NLS-2$
        result = result.replaceAll("__(.+?)__", "$1"); //$NON-NLS-1$ //$NON-NLS-2$
        result = result.replaceAll("_(.+?)_", "$1"); //$NON-NLS-1$ //$NON-NLS-2$

        // Remove inline code backticks
        result = result.replaceAll("`([^`]+)`", "$1"); //$NON-NLS-1$ //$NON-NLS-2$

        // Remove links
        result = result.replaceAll("\\[(.+?)\\]\\(.+?\\)", "$1"); //$NON-NLS-1$ //$NON-NLS-2$

        return result.trim();
    }

    /**
     * Calculates a simple diff between two strings.
     *
     * <p>Returns a list of changes needed to transform oldText to newText.
     * Uses a simple line-by-line comparison.</p>
     *
     * @param oldText the original text
     * @param newText the new text
     * @return list of changes
     */
    public static List<CodeChange> calculateDiff(String oldText, String newText) {
        List<CodeChange> changes = new ArrayList<>();

        if (oldText == null) {
            oldText = ""; //$NON-NLS-1$
        }
        if (newText == null) {
            newText = ""; //$NON-NLS-1$
        }

        if (oldText.equals(newText)) {
            return changes;
        }

        // Simple approach: single replacement of entire content
        // TODO: Implement more sophisticated line-by-line diff
        changes.add(CodeChange.replace(0, oldText.length(), oldText, newText).build());

        return changes;
    }

    /**
     * Finds the common prefix length between two strings.
     *
     * @param a first string
     * @param b second string
     * @return length of common prefix
     */
    public static int commonPrefixLength(String a, String b) {
        int minLen = Math.min(a.length(), b.length());
        int i = 0;
        while (i < minLen && a.charAt(i) == b.charAt(i)) {
            i++;
        }
        return i;
    }

    /**
     * Finds the common suffix length between two strings.
     *
     * @param a first string
     * @param b second string
     * @return length of common suffix
     */
    public static int commonSuffixLength(String a, String b) {
        int minLen = Math.min(a.length(), b.length());
        int i = 0;
        while (i < minLen && a.charAt(a.length() - 1 - i) == b.charAt(b.length() - 1 - i)) {
            i++;
        }
        return i;
    }

    /**
     * Represents an extracted code block from markdown.
     */
    public static class CodeBlock {
        private final String language;
        private final String code;
        private final int startIndex;
        private final int endIndex;

        public CodeBlock(String language, String code, int startIndex, int endIndex) {
            this.language = language != null ? language : ""; //$NON-NLS-1$
            this.code = code != null ? code : ""; //$NON-NLS-1$
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }

        public String getLanguage() {
            return language;
        }

        public String getCode() {
            return code;
        }

        public int getStartIndex() {
            return startIndex;
        }

        public int getEndIndex() {
            return endIndex;
        }

        /**
         * Returns whether this block has a specified language.
         *
         * @return true if language is specified
         */
        public boolean hasLanguage() {
            return !language.isEmpty();
        }

        /**
         * Returns whether this looks like BSL code.
         *
         * @return true if language is bsl or code contains BSL keywords
         */
        public boolean isBsl() {
            if ("bsl".equalsIgnoreCase(language) || "1c".equalsIgnoreCase(language)) { //$NON-NLS-1$ //$NON-NLS-2$
                return true;
            }
            // Check for common BSL keywords
            String lower = code.toLowerCase();
            return lower.contains("процедура") || lower.contains("функция") //$NON-NLS-1$ //$NON-NLS-2$
                    || lower.contains("procedure") || lower.contains("function") //$NON-NLS-1$ //$NON-NLS-2$
                    || lower.contains("конецпроцедуры") || lower.contains("конецфункции"); //$NON-NLS-1$ //$NON-NLS-2$
        }

        @Override
        public String toString() {
            return String.format("CodeBlock[lang=%s, len=%d]", language, code.length()); //$NON-NLS-1$
        }
    }
}
