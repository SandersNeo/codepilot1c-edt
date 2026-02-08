/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
 */
package com.codepilot1c.ui.theme;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.RGB;

/**
 * Dark theme implementation for Vibe UI.
 *
 * <p>Modern dark color scheme inspired by Cursor, Windsurf, and VS Code dark themes.</p>
 */
class DarkTheme implements VibeTheme {

    private final ThemeManager manager;

    // Color definitions (RGB)
    private static final RGB BACKGROUND = new RGB(11, 18, 32);           // #0B1220 - deep blue-black
    private static final RGB SURFACE = new RGB(17, 24, 39);              // #111827 - gray-900
    private static final RGB SURFACE_ELEVATED = new RGB(31, 41, 55);     // #1F2937 - gray-800
    private static final RGB INPUT_BG = new RGB(17, 24, 39);             // #111827 - gray-900

    private static final RGB TEXT = new RGB(229, 231, 235);              // #E5E7EB - gray-200
    private static final RGB TEXT_MUTED = new RGB(148, 163, 184);        // #94A3B8 - slate-400
    private static final RGB TEXT_INVERTED = new RGB(15, 23, 42);        // #0F172A - slate-900

    private static final RGB BORDER = new RGB(36, 50, 68);               // #243244 - custom
    private static final RGB BORDER_SUBTLE = new RGB(31, 41, 55);        // #1F2937 - gray-800
    private static final RGB BORDER_FOCUS = new RGB(96, 165, 250);       // #60A5FA - blue-400

    private static final RGB ACCENT = new RGB(96, 165, 250);             // #60A5FA - blue-400
    private static final RGB ACCENT_HOVER = new RGB(147, 197, 253);      // #93C5FD - blue-300
    private static final RGB SUCCESS = new RGB(34, 197, 94);             // #22C55E - green-500
    private static final RGB WARNING = new RGB(245, 158, 11);            // #F59E0B - amber-500
    private static final RGB DANGER = new RGB(248, 113, 113);            // #F87171 - red-400

    // Chat colors
    private static final RGB USER_MSG_BG = new RGB(30, 41, 59);          // #1E293B - slate-800
    private static final RGB ASSISTANT_MSG_BG = new RGB(17, 24, 39);     // #111827 - gray-900
    private static final RGB SYSTEM_MSG_BG = new RGB(30, 27, 20);        // custom yellow-tinted dark
    private static final RGB TOOL_CALL_BG = new RGB(20, 30, 26);         // custom green-tinted dark
    private static final RGB TOOL_RESULT_BG = new RGB(31, 41, 55);       // #1F2937 - gray-800

    // Code colors
    private static final RGB CODE_BG = new RGB(15, 23, 42);              // #0F172A - slate-900
    private static final RGB INLINE_CODE_BG = new RGB(30, 41, 59);       // #1E293B - slate-800
    private static final RGB CODE_TEXT = new RGB(226, 232, 240);         // #E2E8F0 - slate-200

    // Diff colors
    private static final RGB DIFF_ADDED = new RGB(20, 50, 35);           // dark green
    private static final RGB DIFF_REMOVED = new RGB(50, 20, 20);         // dark red
    private static final RGB DIFF_CHANGED = new RGB(50, 45, 15);         // dark yellow

    DarkTheme(ThemeManager manager) {
        this.manager = manager;
    }

    // === Background Colors ===

    @Override
    public Color getBackground() {
        return manager.getColor("dark.background", BACKGROUND);
    }

    @Override
    public Color getSurface() {
        return manager.getColor("dark.surface", SURFACE);
    }

    @Override
    public Color getSurfaceElevated() {
        return manager.getColor("dark.surface.elevated", SURFACE_ELEVATED);
    }

    @Override
    public Color getInputBackground() {
        return manager.getColor("dark.input.bg", INPUT_BG);
    }

    // === Text Colors ===

    @Override
    public Color getText() {
        return manager.getColor("dark.text", TEXT);
    }

    @Override
    public Color getTextMuted() {
        return manager.getColor("dark.text.muted", TEXT_MUTED);
    }

    @Override
    public Color getTextInverted() {
        return manager.getColor("dark.text.inverted", TEXT_INVERTED);
    }

    // === Border Colors ===

    @Override
    public Color getBorder() {
        return manager.getColor("dark.border", BORDER);
    }

    @Override
    public Color getBorderSubtle() {
        return manager.getColor("dark.border.subtle", BORDER_SUBTLE);
    }

    @Override
    public Color getBorderFocus() {
        return manager.getColor("dark.border.focus", BORDER_FOCUS);
    }

    // === Semantic Colors ===

    @Override
    public Color getAccent() {
        return manager.getColor("dark.accent", ACCENT);
    }

    @Override
    public Color getAccentHover() {
        return manager.getColor("dark.accent.hover", ACCENT_HOVER);
    }

    @Override
    public Color getSuccess() {
        return manager.getColor("dark.success", SUCCESS);
    }

    @Override
    public Color getWarning() {
        return manager.getColor("dark.warning", WARNING);
    }

    @Override
    public Color getDanger() {
        return manager.getColor("dark.danger", DANGER);
    }

    // === Chat Colors ===

    @Override
    public Color getUserMessageBackground() {
        return manager.getColor("dark.chat.user", USER_MSG_BG);
    }

    @Override
    public Color getAssistantMessageBackground() {
        return manager.getColor("dark.chat.assistant", ASSISTANT_MSG_BG);
    }

    @Override
    public Color getSystemMessageBackground() {
        return manager.getColor("dark.chat.system", SYSTEM_MSG_BG);
    }

    @Override
    public Color getToolCallBackground() {
        return manager.getColor("dark.chat.tool.call", TOOL_CALL_BG);
    }

    @Override
    public Color getToolResultBackground() {
        return manager.getColor("dark.chat.tool.result", TOOL_RESULT_BG);
    }

    // === Code Colors ===

    @Override
    public Color getCodeBackground() {
        return manager.getColor("dark.code.bg", CODE_BG);
    }

    @Override
    public Color getInlineCodeBackground() {
        return manager.getColor("dark.code.inline.bg", INLINE_CODE_BG);
    }

    @Override
    public Color getCodeText() {
        return manager.getColor("dark.code.text", CODE_TEXT);
    }

    // === Diff Colors ===

    @Override
    public Color getDiffAddedBackground() {
        return manager.getColor("dark.diff.added", DIFF_ADDED);
    }

    @Override
    public Color getDiffRemovedBackground() {
        return manager.getColor("dark.diff.removed", DIFF_REMOVED);
    }

    @Override
    public Color getDiffChangedBackground() {
        return manager.getColor("dark.diff.changed", DIFF_CHANGED);
    }

    // === Fonts ===

    @Override
    public Font getFont() {
        return manager.getFont("font.default",
                manager.getBaseFontName(),
                manager.getBaseFontHeight(),
                SWT.NORMAL);
    }

    @Override
    public Font getFontBold() {
        return manager.getFont("font.bold",
                manager.getBaseFontName(),
                manager.getBaseFontHeight(),
                SWT.BOLD);
    }

    @Override
    public Font getFontItalic() {
        return manager.getFont("font.italic",
                manager.getBaseFontName(),
                manager.getBaseFontHeight(),
                SWT.ITALIC);
    }

    @Override
    public Font getFontMono() {
        return manager.getFont("font.mono",
                manager.getMonoFontName(),
                manager.getBaseFontHeight(),
                SWT.NORMAL);
    }

    @Override
    public Font getFontMonoBold() {
        return manager.getFont("font.mono.bold",
                manager.getMonoFontName(),
                manager.getBaseFontHeight(),
                SWT.BOLD);
    }

    @Override
    public Font getFontHeader() {
        return manager.getFont("font.header",
                manager.getBaseFontName(),
                manager.getBaseFontHeight() + 2,
                SWT.BOLD);
    }

    @Override
    public Font getFontSmall() {
        return manager.getFont("font.small",
                manager.getBaseFontName(),
                Math.max(manager.getBaseFontHeight() - 2, 8),
                SWT.NORMAL);
    }

    // === Spacing ===

    @Override
    public int getMargin() {
        return 8;
    }

    @Override
    public int getMarginSmall() {
        return 4;
    }

    @Override
    public int getMarginLarge() {
        return 16;
    }

    @Override
    public int getPadding() {
        return 12;
    }

    @Override
    public int getBorderRadius() {
        return 8;
    }

    // === Theme Info ===

    @Override
    public boolean isDark() {
        return true;
    }

    @Override
    public String getName() {
        return "Vibe Dark";
    }

    // === Utility ===

    @Override
    public Color withAlpha(Color base, float alpha) {
        RGB bg = BACKGROUND;
        int r = Math.round(base.getRed() * alpha + bg.red * (1 - alpha));
        int g = Math.round(base.getGreen() * alpha + bg.green * (1 - alpha));
        int b = Math.round(base.getBlue() * alpha + bg.blue * (1 - alpha));
        return manager.getColor("blend." + base.hashCode() + "." + alpha,
                new RGB(r, g, b));
    }

    @Override
    public RGB getRGB(String token) {
        switch (token) {
            case "background": return BACKGROUND;
            case "surface": return SURFACE;
            case "text": return TEXT;
            case "text.muted": return TEXT_MUTED;
            case "border": return BORDER;
            case "accent": return ACCENT;
            case "success": return SUCCESS;
            case "warning": return WARNING;
            case "danger": return DANGER;
            case "code.bg": return CODE_BG;
            default: return null;
        }
    }
}
