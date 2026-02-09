/*
 * Copyright (c) 2024 Example
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.codepilot1c.ui.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;

import com.codepilot1c.ui.editor.CodeApplicationService;
import com.codepilot1c.ui.internal.Messages;
import com.codepilot1c.ui.theme.ThemeManager;
import com.codepilot1c.ui.theme.VibeTheme;

/**
 * Modern code block widget with syntax highlighting and actions.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Theme-aware colors</li>
 *   <li>Horizontal scrolling (no wrap)</li>
 *   <li>Collapsible for long code</li>
 *   <li>Copy/Insert/Replace/Expand actions</li>
 *   <li>Line numbers (optional)</li>
 * </ul>
 */
public class CodeBlockWidget extends Composite {

    private static final int MAX_VISIBLE_LINES = 15;
    private static final int MIN_VISIBLE_LINES = 3;

    private final String code;
    private final String language;
    private final VibeTheme theme;

    private StyledText codeText;
    private Composite headerComposite;
    private Label expandLabel;
    private boolean expanded = false;
    private int totalLines;

    /**
     * Creates a new code block widget.
     *
     * @param parent the parent composite
     * @param code the code to display
     * @param language the programming language (can be null)
     */
    public CodeBlockWidget(Composite parent, String code, String language) {
        super(parent, SWT.NONE);
        this.code = code != null ? code : "";
        this.language = language;
        this.theme = ThemeManager.getInstance().getTheme();
        this.totalLines = this.code.split("\n", -1).length;

        createContents();
    }

    private void createContents() {
        // Main layout
        GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.verticalSpacing = 0;
        setLayout(layout);
        setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        // Border paint
        addListener(SWT.Paint, this::paintBorder);

        // Header with language and buttons
        createHeader();

        // Code area with horizontal scroll
        createCodeArea();
    }

    private void createHeader() {
        headerComposite = new Composite(this, SWT.NONE);
        headerComposite.setBackground(theme.getCodeBackground());

        GridLayout headerLayout = new GridLayout(6, false);
        headerLayout.marginWidth = theme.getPadding();
        headerLayout.marginHeight = theme.getMarginSmall();
        headerLayout.horizontalSpacing = theme.getMargin();
        headerComposite.setLayout(headerLayout);
        headerComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        // Language label
        Label langLabel = new Label(headerComposite, SWT.NONE);
        langLabel.setBackground(headerComposite.getBackground());
        langLabel.setForeground(theme.getTextMuted());
        langLabel.setFont(theme.getFontSmall());
        if (language != null && !language.isEmpty()) {
            langLabel.setText(language.toUpperCase());
        } else {
            langLabel.setText("CODE");
        }
        langLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        // Line count
        Label linesLabel = new Label(headerComposite, SWT.NONE);
        linesLabel.setBackground(headerComposite.getBackground());
        linesLabel.setForeground(theme.getTextMuted());
        linesLabel.setFont(theme.getFontSmall());
        linesLabel.setText(totalLines + " " + (totalLines == 1 ? "строка" : "строк"));

        // Copy button
        createActionButton(headerComposite, "\u2398", Messages.CodeBlockWidget_Copy, this::copyToClipboard);

        // Insert button
        createActionButton(headerComposite, "\u2913", Messages.CodeBlockWidget_Insert, this::insertCode);

        // Replace button
        createActionButton(headerComposite, "\u21C4", Messages.CodeBlockWidget_Replace, this::replaceCode);

        // Expand/collapse button (if needed)
        if (totalLines > MAX_VISIBLE_LINES) {
            expandLabel = createActionButton(headerComposite, "\u25BC", "Развернуть", this::toggleExpand);
        }
    }

    private Label createActionButton(Composite parent, String icon, String tooltip, Runnable action) {
        Label btn = new Label(parent, SWT.NONE);
        btn.setText(icon);
        btn.setToolTipText(tooltip);
        btn.setFont(theme.getFont());
        btn.setForeground(theme.getTextMuted());
        btn.setBackground(parent.getBackground());
        btn.setCursor(getDisplay().getSystemCursor(SWT.CURSOR_HAND));

        btn.addMouseTrackListener(new MouseTrackAdapter() {
            @Override
            public void mouseEnter(MouseEvent e) {
                btn.setForeground(theme.getAccent());
            }

            @Override
            public void mouseExit(MouseEvent e) {
                btn.setForeground(theme.getTextMuted());
            }
        });

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseUp(MouseEvent e) {
                action.run();
            }
        });

        return btn;
    }

    private void createCodeArea() {
        // Use StyledText without wrap for proper code display
        // Horizontal scrollbar appears when needed
        codeText = new StyledText(this, SWT.MULTI | SWT.READ_ONLY | SWT.H_SCROLL | SWT.V_SCROLL);
        codeText.setText(code);
        codeText.setEditable(false);
        codeText.setBackground(theme.getCodeBackground());
        codeText.setForeground(theme.getCodeText());
        codeText.setFont(theme.getFontMono());

        // Calculate height
        int visibleLines = Math.min(totalLines, expanded ? totalLines : MAX_VISIBLE_LINES);
        visibleLines = Math.max(visibleLines, MIN_VISIBLE_LINES);

        GridData codeData = new GridData(SWT.FILL, SWT.TOP, true, false);
        codeData.heightHint = visibleLines * codeText.getLineHeight() + theme.getPadding() * 2;
        codeText.setLayoutData(codeData);

        // Padding inside the text area
        codeText.setMargins(theme.getPadding(), theme.getMargin(), theme.getPadding(), theme.getMargin());

        // Apply basic syntax highlighting if language is recognized
        if (language != null) {
            applySyntaxHighlighting();
        }
    }

    private void applySyntaxHighlighting() {
        // MVP: highlight keywords, strings, comments based on language
        // Full syntax highlighting would use a proper lexer

        String lang = language.toLowerCase();
        if (lang.equals("bsl") || lang.equals("1c")) {
            highlightBsl();
        } else if (lang.equals("java")) {
            highlightJava();
        } else if (lang.equals("javascript") || lang.equals("js") || lang.equals("typescript") || lang.equals("ts")) {
            highlightJavaScript();
        } else if (lang.equals("python") || lang.equals("py")) {
            highlightPython();
        }
        // Add more languages as needed
    }

    private void highlightBsl() {
        // BSL/1C keywords
        String[] keywords = {
            "Процедура", "КонецПроцедуры", "Функция", "КонецФункции",
            "Если", "Тогда", "Иначе", "ИначеЕсли", "КонецЕсли",
            "Для", "Каждого", "Из", "По", "Цикл", "КонецЦикла", "Пока",
            "Попытка", "Исключение", "КонецПопытки", "ВызватьИсключение",
            "Возврат", "Перем", "Новый", "И", "Или", "Не",
            "Истина", "Ложь", "Неопределено", "NULL",
            "Procedure", "EndProcedure", "Function", "EndFunction",
            "If", "Then", "Else", "ElsIf", "EndIf",
            "For", "Each", "In", "To", "Do", "EndDo", "While",
            "Try", "Except", "EndTry", "Raise",
            "Return", "Var", "New", "And", "Or", "Not",
            "True", "False", "Undefined"
        };
        highlightKeywords(keywords, theme.getAccent());
    }

    private void highlightJava() {
        String[] keywords = {
            "public", "private", "protected", "class", "interface", "enum",
            "extends", "implements", "static", "final", "abstract",
            "void", "int", "long", "double", "float", "boolean", "char", "byte", "short",
            "if", "else", "for", "while", "do", "switch", "case", "default", "break", "continue",
            "try", "catch", "finally", "throw", "throws",
            "return", "new", "this", "super", "null", "true", "false",
            "import", "package", "instanceof"
        };
        highlightKeywords(keywords, theme.getAccent());
    }

    private void highlightJavaScript() {
        String[] keywords = {
            "const", "let", "var", "function", "class", "extends",
            "if", "else", "for", "while", "do", "switch", "case", "default", "break", "continue",
            "try", "catch", "finally", "throw",
            "return", "new", "this", "super", "null", "undefined", "true", "false",
            "import", "export", "from", "default", "async", "await",
            "typeof", "instanceof", "in", "of"
        };
        highlightKeywords(keywords, theme.getAccent());
    }

    private void highlightPython() {
        String[] keywords = {
            "def", "class", "if", "elif", "else", "for", "while",
            "try", "except", "finally", "raise", "with", "as",
            "return", "yield", "import", "from", "global", "nonlocal",
            "True", "False", "None", "and", "or", "not", "in", "is",
            "lambda", "pass", "break", "continue", "assert"
        };
        highlightKeywords(keywords, theme.getAccent());
    }

    private void highlightKeywords(String[] keywords, org.eclipse.swt.graphics.Color color) {
        String text = codeText.getText();
        for (String keyword : keywords) {
            int index = 0;
            while ((index = text.indexOf(keyword, index)) >= 0) {
                // Check word boundaries
                boolean validStart = index == 0 || !Character.isLetterOrDigit(text.charAt(index - 1));
                boolean validEnd = index + keyword.length() >= text.length()
                        || !Character.isLetterOrDigit(text.charAt(index + keyword.length()));

                if (validStart && validEnd) {
                    org.eclipse.swt.custom.StyleRange style = new org.eclipse.swt.custom.StyleRange();
                    style.start = index;
                    style.length = keyword.length();
                    style.foreground = color;
                    style.fontStyle = SWT.BOLD;
                    codeText.setStyleRange(style);
                }
                index += keyword.length();
            }
        }
    }

    private void paintBorder(Event e) {
        GC gc = e.gc;
        Rectangle bounds = getClientArea();

        // Draw border
        gc.setForeground(theme.getBorder());
        gc.setLineWidth(1);
        gc.drawRectangle(0, 0, bounds.width - 1, bounds.height - 1);
    }

    private void toggleExpand() {
        expanded = !expanded;
        if (expandLabel != null && !expandLabel.isDisposed()) {
            expandLabel.setText(expanded ? "\u25B2" : "\u25BC"); // ▲ or ▼
            expandLabel.setToolTipText(expanded ? "Свернуть" : "Развернуть");
        }

        // Recalculate height
        int visibleLines = expanded ? totalLines : MAX_VISIBLE_LINES;
        visibleLines = Math.max(visibleLines, MIN_VISIBLE_LINES);

        GridData codeData = (GridData) codeText.getLayoutData();
        codeData.heightHint = visibleLines * codeText.getLineHeight() + theme.getPadding() * 2;

        // Relayout parent
        Composite parent = getParent();
        while (parent != null && !(parent instanceof ScrolledComposite)) {
            parent.layout(true, true);
            parent = parent.getParent();
        }
        if (parent instanceof ScrolledComposite) {
            ((ScrolledComposite) parent).setMinSize(((ScrolledComposite) parent).getContent().computeSize(SWT.DEFAULT, SWT.DEFAULT));
        }
    }

    private void copyToClipboard() {
        Clipboard clipboard = new Clipboard(getDisplay());
        try {
            TextTransfer textTransfer = TextTransfer.getInstance();
            clipboard.setContents(new Object[]{code}, new Transfer[]{textTransfer});
        } finally {
            clipboard.dispose();
        }
    }

    private void insertCode() {
        CodeApplicationService service = CodeApplicationService.getInstance();
        service.insertAtCursor(code);
    }

    private void replaceCode() {
        CodeApplicationService service = CodeApplicationService.getInstance();
        service.replaceSelection(code);
    }

    /**
     * Returns the code content.
     *
     * @return the code
     */
    public String getCode() {
        return code;
    }

    /**
     * Returns the language.
     *
     * @return the language
     */
    public String getLanguage() {
        return language;
    }

    @Override
    public void dispose() {
        // No resources to dispose - all managed by ThemeManager
        super.dispose();
    }
}
