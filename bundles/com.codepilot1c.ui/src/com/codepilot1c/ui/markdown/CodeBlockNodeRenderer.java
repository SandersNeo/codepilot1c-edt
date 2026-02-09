/*
 * Copyright (c) 2024 Example
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.codepilot1c.ui.markdown;

import java.util.HashSet;
import java.util.Set;

import com.vladsch.flexmark.ast.Code;
import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.ast.IndentedCodeBlock;
import com.vladsch.flexmark.html.HtmlWriter;
import com.vladsch.flexmark.html.renderer.NodeRenderer;
import com.vladsch.flexmark.html.renderer.NodeRendererContext;
import com.vladsch.flexmark.html.renderer.NodeRendererFactory;
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler;
import com.vladsch.flexmark.util.data.DataHolder;

/**
 * Кастомный рендерер блоков кода с заголовком и кнопкой копирования.
 * Используется совместно с FlexmarkParser для корректного рендеринга
 * блоков кода в HTML без проблем с плейсхолдерами.
 */
public class CodeBlockNodeRenderer implements NodeRenderer {

    public CodeBlockNodeRenderer(DataHolder options) {
        // Options can be used for configuration if needed
    }

    @Override
    public Set<NodeRenderingHandler<?>> getNodeRenderingHandlers() {
        Set<NodeRenderingHandler<?>> handlers = new HashSet<>();
        handlers.add(new NodeRenderingHandler<>(FencedCodeBlock.class, this::renderFencedCodeBlock));
        handlers.add(new NodeRenderingHandler<>(IndentedCodeBlock.class, this::renderIndentedCodeBlock));
        handlers.add(new NodeRenderingHandler<>(Code.class, this::renderInlineCode));
        return handlers;
    }

    /**
     * Рендерит огражденный блок кода (```lang ... ```)
     */
    private void renderFencedCodeBlock(FencedCodeBlock node, NodeRendererContext context, HtmlWriter html) {
        String language = node.getInfo() != null ? node.getInfo().toString().trim() : ""; //$NON-NLS-1$
        String code = node.getContentChars() != null ? node.getContentChars().toString() : ""; //$NON-NLS-1$
        renderCodeBlock(html, language, code);
    }

    /**
     * Рендерит индентированный блок кода (4 пробела)
     */
    private void renderIndentedCodeBlock(IndentedCodeBlock node, NodeRendererContext context, HtmlWriter html) {
        String code = node.getContentChars() != null ? node.getContentChars().toString() : ""; //$NON-NLS-1$
        renderCodeBlock(html, "", code); //$NON-NLS-1$
    }

    /**
     * Рендерит инлайн код (`code`)
     */
    private void renderInlineCode(Code node, NodeRendererContext context, HtmlWriter html) {
        String code = node.getText() != null ? node.getText().toString() : ""; //$NON-NLS-1$
        html.raw("<code class=\"inline\">"); //$NON-NLS-1$
        html.raw(escapeHtml(code));
        html.raw("</code>"); //$NON-NLS-1$
    }

    /**
     * Рендерит блок кода с заголовком и кнопкой копирования.
     */
    private void renderCodeBlock(HtmlWriter html, String language, String code) {
        html.line();
        html.raw("<div class=\"code-block\""); //$NON-NLS-1$
        if (language != null && !language.isEmpty()) {
            html.raw(" data-language=\"").raw(escapeAttribute(language)).raw("\""); //$NON-NLS-1$ //$NON-NLS-2$
        }
        html.raw(">"); //$NON-NLS-1$

        // Header с языком и кнопкой копирования
        html.raw("<div class=\"code-header\">"); //$NON-NLS-1$
        if (language != null && !language.isEmpty()) {
            html.raw("<span class=\"code-lang\">").raw(escapeHtml(language)).raw("</span>"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        html.raw("<button class=\"copy-btn\" onclick=\"copyCode(this)\">Копировать</button>"); //$NON-NLS-1$
        html.raw("</div>"); //$NON-NLS-1$

        // Код
        html.raw("<pre><code"); //$NON-NLS-1$
        if (language != null && !language.isEmpty()) {
            html.raw(" class=\"language-").raw(escapeAttribute(language)).raw("\""); //$NON-NLS-1$ //$NON-NLS-2$
        }
        html.raw(">"); //$NON-NLS-1$
        html.raw(escapeHtml(code.trim()));
        html.raw("</code></pre>"); //$NON-NLS-1$

        html.raw("</div>"); //$NON-NLS-1$
        html.line();
    }

    /**
     * Экранирует HTML-спецсимволы в тексте.
     */
    private String escapeHtml(String text) {
        if (text == null) return ""; //$NON-NLS-1$
        return text
                .replace("&", "&amp;") //$NON-NLS-1$ //$NON-NLS-2$
                .replace("<", "&lt;") //$NON-NLS-1$ //$NON-NLS-2$
                .replace(">", "&gt;"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * Экранирует HTML-спецсимволы в атрибутах.
     */
    private String escapeAttribute(String text) {
        if (text == null) return ""; //$NON-NLS-1$
        return text
                .replace("&", "&amp;") //$NON-NLS-1$ //$NON-NLS-2$
                .replace("<", "&lt;") //$NON-NLS-1$ //$NON-NLS-2$
                .replace(">", "&gt;") //$NON-NLS-1$ //$NON-NLS-2$
                .replace("\"", "&quot;") //$NON-NLS-1$ //$NON-NLS-2$
                .replace("'", "&#39;"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * Фабрика для создания CodeBlockNodeRenderer.
     */
    public static class Factory implements NodeRendererFactory {
        @Override
        public NodeRenderer apply(DataHolder options) {
            return new CodeBlockNodeRenderer(options);
        }
    }
}
