/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
 */
package com.codepilot1c.ui.theme;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.RGB;

/**
 * Theme interface for Vibe UI components.
 *
 * <p>Provides semantic color and font tokens that adapt to light/dark themes.
 * All UI components should use these tokens instead of hardcoded values.</p>
 */
public interface VibeTheme {

    // === Background Colors ===

    /** Main background color */
    Color getBackground();

    /** Surface/card background color */
    Color getSurface();

    /** Elevated surface (hover, selected) */
    Color getSurfaceElevated();

    /** Input field background */
    Color getInputBackground();

    // === Text Colors ===

    /** Primary text color */
    Color getText();

    /** Secondary/muted text color */
    Color getTextMuted();

    /** Inverted text (on accent backgrounds) */
    Color getTextInverted();

    // === Border Colors ===

    /** Default border color */
    Color getBorder();

    /** Subtle border (dividers) */
    Color getBorderSubtle();

    /** Focus border */
    Color getBorderFocus();

    // === Semantic Colors ===

    /** Primary accent color */
    Color getAccent();

    /** Accent hover state */
    Color getAccentHover();

    /** Success color */
    Color getSuccess();

    /** Warning color */
    Color getWarning();

    /** Danger/error color */
    Color getDanger();

    // === Chat-specific Colors ===

    /** User message background */
    Color getUserMessageBackground();

    /** Assistant message background */
    Color getAssistantMessageBackground();

    /** System message background */
    Color getSystemMessageBackground();

    /** Tool call background */
    Color getToolCallBackground();

    /** Tool result background */
    Color getToolResultBackground();

    // === Code Colors ===

    /** Code block background */
    Color getCodeBackground();

    /** Inline code background */
    Color getInlineCodeBackground();

    /** Code text color */
    Color getCodeText();

    // === Diff Colors ===

    /** Added line background */
    Color getDiffAddedBackground();

    /** Removed line background */
    Color getDiffRemovedBackground();

    /** Changed line background */
    Color getDiffChangedBackground();

    // === Fonts ===

    /** Default UI font */
    Font getFont();

    /** Bold font */
    Font getFontBold();

    /** Italic font */
    Font getFontItalic();

    /** Monospace font for code */
    Font getFontMono();

    /** Monospace bold font */
    Font getFontMonoBold();

    /** Header font (larger, bold) */
    Font getFontHeader();

    /** Small font for labels/timestamps */
    Font getFontSmall();

    // === Spacing & Sizing ===

    /** Standard margin (8px) */
    int getMargin();

    /** Small margin (4px) */
    int getMarginSmall();

    /** Large margin (16px) */
    int getMarginLarge();

    /** Standard padding (12px) */
    int getPadding();

    /** Border radius for cards */
    int getBorderRadius();

    // === Theme Info ===

    /** Returns true if this is a dark theme */
    boolean isDark();

    /** Returns theme name */
    String getName();

    // === Utility Methods ===

    /**
     * Creates a color with alpha blending towards background.
     *
     * @param base the base color
     * @param alpha alpha value (0.0 - 1.0)
     * @return blended color
     */
    Color withAlpha(Color base, float alpha);

    /**
     * Returns RGB values for a semantic color token.
     * Useful for creating new Color instances if needed.
     *
     * @param token the color token name
     * @return RGB values or null if token not found
     */
    RGB getRGB(String token);
}
