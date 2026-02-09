/*
 * Copyright (c) 2024 Example
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.codepilot1c.ui.views;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import com.codepilot1c.ui.chat.MessageContentParser;
import com.codepilot1c.ui.chat.MessageKind;
import com.codepilot1c.ui.chat.MessagePart;
import com.codepilot1c.ui.chat.MessagePart.CodeBlockPart;
import com.codepilot1c.ui.chat.MessagePart.TextPart;
import com.codepilot1c.ui.chat.MessagePart.TodoListPart;
import com.codepilot1c.ui.chat.MessagePart.ToolCallPart;
import com.codepilot1c.ui.chat.MessagePart.ToolResultPart;
import com.codepilot1c.ui.theme.ThemeManager;
import com.codepilot1c.ui.theme.VibeTheme;

/**
 * Modern chat message bubble with card-style design.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Rounded card with shadow effect</li>
 *   <li>Role indicator with icon/avatar</li>
 *   <li>Timestamp display</li>
 *   <li>Hover actions (Copy, Regenerate)</li>
 *   <li>Markdown rendering with proper formatting</li>
 *   <li>Theme-aware colors</li>
 * </ul>
 */
public class MessageBubbleComposite extends Composite {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final MessageKind kind;
    private final String rawContent;
    private final LocalDateTime timestamp;
    private final VibeTheme theme;

    private Composite headerComposite;
    private Composite contentComposite;
    private Composite actionsComposite;

    private final List<CodeBlockWidget> codeBlockWidgets = new ArrayList<>();
    private final List<ToolCallWidget> toolCallWidgets = new ArrayList<>();
    private final Map<String, ToolCallWidget> toolCallWidgetMap = new HashMap<>();

    private final MessageContentParser contentParser = new MessageContentParser();
    private boolean hovered = false;

    /**
     * Creates a new message bubble.
     *
     * @param parent the parent composite
     * @param kind the message kind (USER, ASSISTANT, SYSTEM, TOOL)
     * @param content the message content
     */
    public MessageBubbleComposite(Composite parent, MessageKind kind, String content) {
        this(parent, kind, content, LocalDateTime.now());
    }

    /**
     * Creates a new message bubble with timestamp.
     *
     * @param parent the parent composite
     * @param kind the message kind
     * @param content the message content
     * @param timestamp the message timestamp
     */
    public MessageBubbleComposite(Composite parent, MessageKind kind, String content, LocalDateTime timestamp) {
        super(parent, SWT.NONE);
        this.kind = kind;
        this.rawContent = content != null ? content : "";
        this.timestamp = timestamp;
        this.theme = ThemeManager.getInstance().getTheme();

        createContents();
        setupHoverBehavior();
    }

    private void createContents() {
        // Main layout with padding for "card" effect
        GridLayout mainLayout = new GridLayout(1, false);
        mainLayout.marginWidth = theme.getMargin();
        mainLayout.marginHeight = theme.getMarginSmall();
        mainLayout.verticalSpacing = 0;
        setLayout(mainLayout);
        setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        // Set background based on message kind
        setBackground(getBackgroundColorForKind());

        // Paint rounded border
        addListener(SWT.Paint, this::paintBorder);

        // Header: role + timestamp
        createHeader();

        // Content area
        createContent();

        // Actions (hidden by default, shown on hover)
        createActions();
    }

    private void createHeader() {
        headerComposite = new Composite(this, SWT.NONE);
        GridLayout headerLayout = new GridLayout(3, false);
        headerLayout.marginWidth = theme.getPadding();
        headerLayout.marginHeight = theme.getMarginSmall();
        headerLayout.horizontalSpacing = theme.getMargin();
        headerComposite.setLayout(headerLayout);
        headerComposite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        headerComposite.setBackground(getBackgroundColorForKind());

        // Role icon/indicator
        Label roleIcon = new Label(headerComposite, SWT.NONE);
        roleIcon.setText(getRoleIcon());
        roleIcon.setFont(theme.getFontBold());
        roleIcon.setForeground(getRoleColor());
        roleIcon.setBackground(headerComposite.getBackground());

        // Role name
        Label roleName = new Label(headerComposite, SWT.NONE);
        roleName.setText(kind.getDisplayName());
        roleName.setFont(theme.getFontBold());
        roleName.setForeground(theme.getText());
        roleName.setBackground(headerComposite.getBackground());
        roleName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        // Timestamp
        Label timeLabel = new Label(headerComposite, SWT.NONE);
        timeLabel.setText(timestamp.format(TIME_FORMATTER));
        timeLabel.setFont(theme.getFontSmall());
        timeLabel.setForeground(theme.getTextMuted());
        timeLabel.setBackground(headerComposite.getBackground());
    }

    private void createContent() {
        contentComposite = new Composite(this, SWT.NONE);
        GridLayout contentLayout = new GridLayout(1, false);
        contentLayout.marginWidth = theme.getPadding();
        contentLayout.marginHeight = theme.getMarginSmall();
        contentLayout.verticalSpacing = theme.getMarginSmall();
        contentComposite.setLayout(contentLayout);
        contentComposite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        contentComposite.setBackground(getBackgroundColorForKind());

        // Parse and render content
        List<MessagePart> parts = contentParser.parse(rawContent);
        if (parts.isEmpty() && !rawContent.trim().isEmpty()) {
            // Fallback: render as plain text
            createTextWidget(rawContent);
        } else {
            for (MessagePart part : parts) {
                renderPart(part);
            }
        }
    }

    private void createActions() {
        actionsComposite = new Composite(this, SWT.NONE);
        GridLayout actionsLayout = new GridLayout(3, false);
        actionsLayout.marginWidth = theme.getPadding();
        actionsLayout.marginHeight = theme.getMarginSmall();
        actionsLayout.horizontalSpacing = theme.getMarginSmall();
        actionsComposite.setLayout(actionsLayout);
        actionsComposite.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
        actionsComposite.setBackground(getBackgroundColorForKind());
        actionsComposite.setVisible(false);

        // Copy button
        Label copyBtn = createActionButton(actionsComposite, "\u2398", "Копировать"); // ⎘
        copyBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseUp(MouseEvent e) {
                copyToClipboard();
            }
        });

        // More actions based on message kind
        if (kind == MessageKind.ASSISTANT) {
            Label regenerateBtn = createActionButton(actionsComposite, "\u21BB", "Регенерировать"); // ↻
            // TODO: implement regenerate
        }
    }

    private Label createActionButton(Composite parent, String icon, String tooltip) {
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

        return btn;
    }

    private void setupHoverBehavior() {
        MouseTrackAdapter hoverListener = new MouseTrackAdapter() {
            @Override
            public void mouseEnter(MouseEvent e) {
                setHovered(true);
            }

            @Override
            public void mouseExit(MouseEvent e) {
                // Check if mouse is still within bounds
                Point pt = getDisplay().getCursorLocation();
                pt = toControl(pt);
                Rectangle bounds = getClientArea();
                if (!bounds.contains(pt)) {
                    setHovered(false);
                }
            }
        };

        addMouseTrackListener(hoverListener);
        if (headerComposite != null) headerComposite.addMouseTrackListener(hoverListener);
        if (contentComposite != null) contentComposite.addMouseTrackListener(hoverListener);
    }

    private void setHovered(boolean hovered) {
        if (this.hovered != hovered) {
            this.hovered = hovered;
            if (actionsComposite != null && !actionsComposite.isDisposed()) {
                actionsComposite.setVisible(hovered);
                layout(true, true);
            }
        }
    }

    private void renderPart(MessagePart part) {
        if (part instanceof TextPart textPart) {
            createTextWidget(textPart.content());
        } else if (part instanceof CodeBlockPart codeBlock) {
            createCodeBlockWidget(codeBlock);
        } else if (part instanceof ToolCallPart toolCall) {
            createToolCallWidget(toolCall);
        } else if (part instanceof ToolResultPart toolResult) {
            updateToolCallWithResult(toolResult);
        } else if (part instanceof TodoListPart todoList) {
            createTodoListWidget(todoList);
        }
    }

    private void createTextWidget(String text) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }

        StyledText styledText = new StyledText(contentComposite, SWT.WRAP | SWT.READ_ONLY);
        styledText.setEditable(false);
        styledText.setBackground(getBackgroundColorForKind());
        styledText.setForeground(theme.getText());
        styledText.setFont(theme.getFont());
        styledText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        // Process markdown and apply styles
        List<StyleRange> styles = new ArrayList<>();
        String processedText = processMarkdown(text, styles);
        styledText.setText(processedText);

        for (StyleRange style : styles) {
            if (style.start >= 0 && style.start + style.length <= styledText.getCharCount()) {
                styledText.setStyleRange(style);
            }
        }

        // Context menu for copy
        Menu menu = new Menu(styledText);
        MenuItem copyItem = new MenuItem(menu, SWT.PUSH);
        copyItem.setText("Копировать");
        copyItem.addListener(SWT.Selection, e -> styledText.copy());
        styledText.setMenu(menu);
    }

    private String processMarkdown(String text, List<StyleRange> styles) {
        // Process headers
        text = processPattern(text, styles, "^(#{1,6})\\s+(.+)$",
                (start, content) -> createHeaderStyle(start, content.length()));

        // Process bold
        text = processPattern(text, styles, "\\*\\*(.+?)\\*\\*|__(.+?)__",
                (start, content) -> createBoldStyle(start, content.length()));

        // Process italic
        text = processPattern(text, styles, "(?<![\\*_])\\*([^\\*]+)\\*(?![\\*_])|(?<![\\*_])_([^_]+)_(?![\\*_])",
                (start, content) -> createItalicStyle(start, content.length()));

        // Process inline code
        text = processPattern(text, styles, "`([^`]+)`",
                (start, content) -> createInlineCodeStyle(start, content.length()));

        return text;
    }

    @FunctionalInterface
    private interface StyleFactory {
        StyleRange create(int start, String content);
    }

    private String processPattern(String text, List<StyleRange> styles, String pattern, StyleFactory factory) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.MULTILINE).matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String content = matcher.group(matcher.groupCount());
            if (content == null) {
                for (int i = 1; i < matcher.groupCount(); i++) {
                    if (matcher.group(i) != null) {
                        content = matcher.group(i);
                        break;
                    }
                }
            }
            if (content == null) content = matcher.group(0);

            int startPos = sb.length();
            matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(content));
            styles.add(factory.create(startPos, content));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private StyleRange createHeaderStyle(int start, int length) {
        StyleRange style = new StyleRange();
        style.start = start;
        style.length = length;
        style.font = theme.getFontHeader();
        style.foreground = theme.getAccent();
        return style;
    }

    private StyleRange createBoldStyle(int start, int length) {
        StyleRange style = new StyleRange();
        style.start = start;
        style.length = length;
        style.font = theme.getFontBold();
        style.fontStyle = SWT.BOLD;
        return style;
    }

    private StyleRange createItalicStyle(int start, int length) {
        StyleRange style = new StyleRange();
        style.start = start;
        style.length = length;
        style.font = theme.getFontItalic();
        style.fontStyle = SWT.ITALIC;
        return style;
    }

    private StyleRange createInlineCodeStyle(int start, int length) {
        StyleRange style = new StyleRange();
        style.start = start;
        style.length = length;
        style.font = theme.getFontMono();
        style.background = theme.getInlineCodeBackground();
        return style;
    }

    private void createCodeBlockWidget(CodeBlockPart codeBlock) {
        CodeBlockWidget widget = new CodeBlockWidget(contentComposite, codeBlock.code(), codeBlock.language());
        widget.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        codeBlockWidgets.add(widget);
    }

    private void createToolCallWidget(ToolCallPart toolCall) {
        ToolCallWidget widget = new ToolCallWidget(contentComposite, toolCall);
        widget.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        toolCallWidgets.add(widget);
        toolCallWidgetMap.put(toolCall.toolCallId(), widget);
    }

    private void updateToolCallWithResult(ToolResultPart result) {
        ToolCallWidget widget = toolCallWidgetMap.get(result.toolCallId());
        if (widget != null && !widget.isDisposed()) {
            widget.setResult(result);
        }
    }

    private void createTodoListWidget(TodoListPart todoList) {
        TodoListWidget widget = new TodoListWidget(contentComposite, todoList);
        widget.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    }

    private void paintBorder(Event e) {
        GC gc = e.gc;
        Rectangle bounds = getClientArea();

        // Draw subtle border
        gc.setForeground(theme.getBorderSubtle());
        gc.setLineWidth(1);

        // Top border line
        gc.drawLine(0, 0, bounds.width, 0);

        // Bottom border line
        gc.drawLine(0, bounds.height - 1, bounds.width, bounds.height - 1);
    }

    private Color getBackgroundColorForKind() {
        switch (kind) {
            case USER:
                return theme.getUserMessageBackground();
            case ASSISTANT:
                return theme.getAssistantMessageBackground();
            case SYSTEM:
                return theme.getSystemMessageBackground();
            case TOOL_CALL:
                return theme.getToolCallBackground();
            case TOOL_RESULT:
                return theme.getToolResultBackground();
            default:
                return theme.getSurface();
        }
    }

    private String getRoleIcon() {
        switch (kind) {
            case USER:
                return "\u2022"; // bullet
            case ASSISTANT:
                return "\u2726"; // sparkle
            case SYSTEM:
                return "\u2699"; // gear
            case TOOL_CALL:
            case TOOL_RESULT:
                return "\u2692"; // hammer and pick
            default:
                return "\u2022";
        }
    }

    private Color getRoleColor() {
        switch (kind) {
            case USER:
                return theme.getAccent();
            case ASSISTANT:
                return theme.getSuccess();
            case SYSTEM:
                return theme.getWarning();
            case TOOL_CALL:
            case TOOL_RESULT:
                return theme.getTextMuted();
            default:
                return theme.getText();
        }
    }

    private void copyToClipboard() {
        org.eclipse.swt.dnd.Clipboard clipboard = new org.eclipse.swt.dnd.Clipboard(getDisplay());
        org.eclipse.swt.dnd.TextTransfer textTransfer = org.eclipse.swt.dnd.TextTransfer.getInstance();
        clipboard.setContents(new Object[]{rawContent}, new org.eclipse.swt.dnd.Transfer[]{textTransfer});
        clipboard.dispose();
    }

    /**
     * Returns the message kind.
     *
     * @return the message kind
     */
    public MessageKind getMessageKind() {
        return kind;
    }

    /**
     * Returns the raw content.
     *
     * @return the raw content
     */
    public String getRawContent() {
        return rawContent;
    }

    /**
     * Returns the code block widgets.
     *
     * @return list of code block widgets
     */
    public List<CodeBlockWidget> getCodeBlockWidgets() {
        return new ArrayList<>(codeBlockWidgets);
    }

    /**
     * Updates a tool call with its result.
     *
     * @param result the tool result
     */
    public void addToolResult(ToolResultPart result) {
        updateToolCallWithResult(result);
    }

    @Override
    public void dispose() {
        codeBlockWidgets.clear();
        toolCallWidgets.clear();
        toolCallWidgetMap.clear();
        super.dispose();
    }
}
