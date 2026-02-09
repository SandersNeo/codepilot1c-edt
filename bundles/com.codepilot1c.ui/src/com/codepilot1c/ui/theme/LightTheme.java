/*
 * Copyright (c) 2024 Example
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.codepilot1c.ui.theme;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.RGB;

/**
 * Light theme implementation for Vibe UI.
 *
 * <p>Clean, modern light color scheme inspired by VS Code, Windsurf, and Tailwind.</p>
 */
class LightTheme implements VibeTheme {

    private final ThemeManager manager;

    // Color definitions (RGB)
    private static final RGB BACKGROUND = new RGB(248, 250, 252);        // #F8FAFC - slate-50
    private static final RGB SURFACE = new RGB(255, 255, 255);           // #FFFFFF - white
    private static final RGB SURFACE_ELEVATED = new RGB(241, 245, 249);  // #F1F5F9 - slate-100
    private static final RGB INPUT_BG = new RGB(255, 255, 255);          // #FFFFFF

    private static final RGB TEXT = new RGB(15, 23, 42);                 // #0F172A - slate-900
    private static final RGB TEXT_MUTED = new RGB(100, 116, 139);        // #64748B - slate-500
    private static final RGB TEXT_INVERTED = new RGB(255, 255, 255);     // #FFFFFF

    private static final RGB BORDER = new RGB(226, 232, 240);            // #E2E8F0 - slate-200
    private static final RGB BORDER_SUBTLE = new RGB(241, 245, 249);     // #F1F5F9 - slate-100
    private static final RGB BORDER_FOCUS = new RGB(59, 130, 246);       // #3B82F6 - blue-500

    private static final RGB ACCENT = new RGB(37, 99, 235);              // #2563EB - blue-600
    private static final RGB ACCENT_HOVER = new RGB(29, 78, 216);        // #1D4ED8 - blue-700
    private static final RGB SUCCESS = new RGB(22, 163, 74);             // #16A34A - green-600
    private static final RGB WARNING = new RGB(217, 119, 6);             // #D97706 - amber-600
    private static final RGB DANGER = new RGB(220, 38, 38);              // #DC2626 - red-600

    // Chat colors
    private static final RGB USER_MSG_BG = new RGB(239, 246, 255);       // #EFF6FF - blue-50
    private static final RGB ASSISTANT_MSG_BG = new RGB(255, 255, 255);  // #FFFFFF
    private static final RGB SYSTEM_MSG_BG = new RGB(254, 252, 232);     // #FEFCE8 - yellow-50
    private static final RGB TOOL_CALL_BG = new RGB(240, 253, 244);      // #F0FDF4 - green-50
    private static final RGB TOOL_RESULT_BG = new RGB(241, 245, 249);    // #F1F5F9 - slate-100

    // Code colors
    private static final RGB CODE_BG = new RGB(248, 250, 252);           // #F8FAFC - slate-50
    private static final RGB INLINE_CODE_BG = new RGB(241, 245, 249);    // #F1F5F9 - slate-100
    private static final RGB CODE_TEXT = new RGB(51, 65, 85);            // #334155 - slate-700

    // Diff colors
    private static final RGB DIFF_ADDED = new RGB(220, 252, 231);        // #DCFCE7 - green-100
    private static final RGB DIFF_REMOVED = new RGB(254, 226, 226);      // #FEE2E2 - red-100
    private static final RGB DIFF_CHANGED = new RGB(254, 249, 195);      // #FEF9C3 - yellow-100

    LightTheme(ThemeManager manager) {
        this.manager = manager;
    }

    // === Background Colors ===

    @Override
    public Color getBackground() {
        return manager.getColor("light.background", BACKGROUND);
    }

    @Override
    public Color getSurface() {
        return manager.getColor("light.surface", SURFACE);
    }

    @Override
    public Color getSurfaceElevated() {
        return manager.getColor("light.surface.elevated", SURFACE_ELEVATED);
    }

    @Override
    public Color getInputBackground() {
        return manager.getColor("light.input.bg", INPUT_BG);
    }

    // === Text Colors ===

    @Override
    public Color getText() {
        return manager.getColor("light.text", TEXT);
    }

    @Override
    public Color getTextMuted() {
        return manager.getColor("light.text.muted", TEXT_MUTED);
    }

    @Override
    public Color getTextInverted() {
        return manager.getColor("light.text.inverted", TEXT_INVERTED);
    }

    // === Border Colors ===

    @Override
    public Color getBorder() {
        return manager.getColor("light.border", BORDER);
    }

    @Override
    public Color getBorderSubtle() {
        return manager.getColor("light.border.subtle", BORDER_SUBTLE);
    }

    @Override
    public Color getBorderFocus() {
        return manager.getColor("light.border.focus", BORDER_FOCUS);
    }

    // === Semantic Colors ===

    @Override
    public Color getAccent() {
        return manager.getColor("light.accent", ACCENT);
    }

    @Override
    public Color getAccentHover() {
        return manager.getColor("light.accent.hover", ACCENT_HOVER);
    }

    @Override
    public Color getSuccess() {
        return manager.getColor("light.success", SUCCESS);
    }

    @Override
    public Color getWarning() {
        return manager.getColor("light.warning", WARNING);
    }

    @Override
    public Color getDanger() {
        return manager.getColor("light.danger", DANGER);
    }

    // === Chat Colors ===

    @Override
    public Color getUserMessageBackground() {
        return manager.getColor("light.chat.user", USER_MSG_BG);
    }

    @Override
    public Color getAssistantMessageBackground() {
        return manager.getColor("light.chat.assistant", ASSISTANT_MSG_BG);
    }

    @Override
    public Color getSystemMessageBackground() {
        return manager.getColor("light.chat.system", SYSTEM_MSG_BG);
    }

    @Override
    public Color getToolCallBackground() {
        return manager.getColor("light.chat.tool.call", TOOL_CALL_BG);
    }

    @Override
    public Color getToolResultBackground() {
        return manager.getColor("light.chat.tool.result", TOOL_RESULT_BG);
    }

    // === Code Colors ===

    @Override
    public Color getCodeBackground() {
        return manager.getColor("light.code.bg", CODE_BG);
    }

    @Override
    public Color getInlineCodeBackground() {
        return manager.getColor("light.code.inline.bg", INLINE_CODE_BG);
    }

    @Override
    public Color getCodeText() {
        return manager.getColor("light.code.text", CODE_TEXT);
    }

    // === Diff Colors ===

    @Override
    public Color getDiffAddedBackground() {
        return manager.getColor("light.diff.added", DIFF_ADDED);
    }

    @Override
    public Color getDiffRemovedBackground() {
        return manager.getColor("light.diff.removed", DIFF_REMOVED);
    }

    @Override
    public Color getDiffChangedBackground() {
        return manager.getColor("light.diff.changed", DIFF_CHANGED);
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
        return false;
    }

    @Override
    public String getName() {
        return "Vibe Light";
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
