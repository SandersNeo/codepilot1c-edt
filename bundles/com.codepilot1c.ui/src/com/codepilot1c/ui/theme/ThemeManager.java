/*
 * Copyright (c) 2024 Example
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.codepilot1c.ui.theme;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.themes.ITheme;
import org.eclipse.ui.themes.IThemeManager;

import com.codepilot1c.core.logging.VibeLogger;

/**
 * Central theme manager for Vibe UI components.
 *
 * <p>Manages color and font resources, responds to Eclipse theme changes,
 * and provides a consistent theming API for all Vibe widgets.</p>
 *
 * <p>Usage:</p>
 * <pre>
 * VibeTheme theme = ThemeManager.getInstance().getTheme();
 * Color bg = theme.getBackground();
 * Font mono = theme.getFontMono();
 * </pre>
 */
public class ThemeManager {

    private static final VibeLogger.CategoryLogger LOG = VibeLogger.forClass(ThemeManager.class);

    private static ThemeManager instance;

    private final Map<String, Color> colorCache = new HashMap<>();
    private final Map<String, Font> fontCache = new HashMap<>();
    private final List<Consumer<VibeTheme>> themeChangeListeners = new ArrayList<>();

    private VibeTheme currentTheme;
    private boolean isDarkTheme;
    private Display display;
    private boolean initialized = false;
    private IPropertyChangeListener eclipseThemeListener;

    private ThemeManager() {
        // Private constructor for singleton
    }

    /**
     * Returns the singleton instance.
     *
     * @return the theme manager
     */
    public static synchronized ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }

    /**
     * Initializes the theme manager.
     * Should be called once during plugin startup.
     * This method is idempotent - calling it multiple times is safe.
     *
     * @param display the display
     */
    public synchronized void initialize(Display display) {
        if (initialized) {
            LOG.debug("ThemeManager already initialized, skipping");
            return;
        }

        this.display = display;
        detectTheme();
        createTheme();

        // Listen for theme changes
        try {
            IThemeManager themeManager = PlatformUI.getWorkbench().getThemeManager();
            eclipseThemeListener = event -> {
                if (IThemeManager.CHANGE_CURRENT_THEME.equals(event.getProperty())) {
                    LOG.info("Eclipse theme changed, updating Vibe theme");
                    detectTheme();
                    createTheme();
                    notifyThemeChange();
                }
            };
            themeManager.addPropertyChangeListener(eclipseThemeListener);
        } catch (Exception e) {
            LOG.warn("Could not register theme change listener: %s", e.getMessage());
        }

        initialized = true;
        LOG.info("ThemeManager initialized, dark mode: %b", isDarkTheme);
    }

    /**
     * Returns the current theme.
     *
     * @return the current theme
     */
    public VibeTheme getTheme() {
        if (currentTheme == null) {
            if (display == null) {
                display = Display.getDefault();
            }
            detectTheme();
            createTheme();
        }
        return currentTheme;
    }

    /**
     * Returns whether the current theme is dark.
     *
     * @return true if dark theme
     */
    public boolean isDarkTheme() {
        return isDarkTheme;
    }

    /**
     * Adds a theme change listener.
     *
     * @param listener the listener
     */
    public void addThemeChangeListener(Consumer<VibeTheme> listener) {
        themeChangeListeners.add(listener);
    }

    /**
     * Removes a theme change listener.
     *
     * @param listener the listener
     */
    public void removeThemeChangeListener(Consumer<VibeTheme> listener) {
        themeChangeListeners.remove(listener);
    }

    /**
     * Gets or creates a color from the cache.
     *
     * @param key the cache key
     * @param rgb the RGB value
     * @return the color
     */
    public Color getColor(String key, RGB rgb) {
        return colorCache.computeIfAbsent(key, k -> new Color(getDisplay(), rgb));
    }

    /**
     * Gets or creates a font from the cache.
     *
     * @param key the cache key
     * @param name font name
     * @param height font height
     * @param style SWT style
     * @return the font
     */
    public Font getFont(String key, String name, int height, int style) {
        return fontCache.computeIfAbsent(key, k -> {
            FontData fd = new FontData(name, height, style);
            return new Font(getDisplay(), fd);
        });
    }

    /**
     * Disposes all cached resources.
     * Should be called during plugin shutdown.
     */
    public synchronized void dispose() {
        // Remove Eclipse theme listener
        if (eclipseThemeListener != null) {
            try {
                IThemeManager themeManager = PlatformUI.getWorkbench().getThemeManager();
                themeManager.removePropertyChangeListener(eclipseThemeListener);
            } catch (Exception e) {
                // Workbench may already be disposed
            }
            eclipseThemeListener = null;
        }

        // Dispose resources on UI thread if possible
        Runnable disposeRunnable = () -> {
            for (Color color : colorCache.values()) {
                if (color != null && !color.isDisposed()) {
                    color.dispose();
                }
            }
            colorCache.clear();

            for (Font font : fontCache.values()) {
                if (font != null && !font.isDisposed()) {
                    font.dispose();
                }
            }
            fontCache.clear();
        };

        if (display != null && !display.isDisposed()) {
            if (Display.getCurrent() == display) {
                disposeRunnable.run();
            } else {
                try {
                    display.syncExec(disposeRunnable);
                } catch (Exception e) {
                    // Display may be disposed, just run directly
                    disposeRunnable.run();
                }
            }
        } else {
            disposeRunnable.run();
        }

        themeChangeListeners.clear();
        currentTheme = null;
        initialized = false;

        LOG.debug("ThemeManager disposed");
    }

    private void detectTheme() {
        try {
            // Try to detect from Eclipse theme
            IThemeManager themeManager = PlatformUI.getWorkbench().getThemeManager();
            ITheme theme = themeManager.getCurrentTheme();
            String themeId = theme.getId();

            // Check if theme ID contains "dark" indicators
            isDarkTheme = themeId.toLowerCase().contains("dark")
                    || themeId.contains("org.eclipse.e4.ui.css.theme.e4_dark");

            LOG.debug("Detected Eclipse theme: %s, isDark: %b", themeId, isDarkTheme);
        } catch (Exception e) {
            // Fallback: check system background color
            try {
                Color sysBg = display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
                // If background is dark (low luminance), assume dark theme
                int luminance = (sysBg.getRed() + sysBg.getGreen() + sysBg.getBlue()) / 3;
                isDarkTheme = luminance < 128;
                LOG.debug("Detected theme from system color, luminance: %d, isDark: %b", luminance, isDarkTheme);
            } catch (Exception e2) {
                isDarkTheme = false;
                LOG.debug("Could not detect theme, defaulting to light");
            }
        }
    }

    private void createTheme() {
        // NOTE: We do NOT dispose old colors here because existing widgets may still
        // hold references to them. Colors are only disposed during plugin shutdown.
        // The cache is cleared so new colors are created for the new theme,
        // but old widgets will continue using their captured theme references.

        currentTheme = isDarkTheme ? new DarkTheme(this) : new LightTheme(this);
    }

    private void notifyThemeChange() {
        for (Consumer<VibeTheme> listener : themeChangeListeners) {
            try {
                listener.accept(currentTheme);
            } catch (Exception e) {
                LOG.warn("Error notifying theme change listener: %s", e.getMessage());
            }
        }
    }

    /**
     * Returns the display.
     *
     * @return the display
     */
    Display getDisplay() {
        return display != null ? display : Display.getDefault();
    }

    /**
     * Returns the base font height.
     *
     * @return font height in points
     */
    int getBaseFontHeight() {
        Font systemFont = getDisplay().getSystemFont();
        FontData[] fontData = systemFont.getFontData();
        return fontData.length > 0 ? fontData[0].getHeight() : 10;
    }

    /**
     * Returns the base font name.
     *
     * @return font name
     */
    String getBaseFontName() {
        Font systemFont = getDisplay().getSystemFont();
        FontData[] fontData = systemFont.getFontData();
        return fontData.length > 0 ? fontData[0].getName() : "Arial";
    }

    /**
     * Returns the monospace font name.
     * Tries Menlo, Consolas, Monaco, or falls back to Monospace.
     *
     * @return monospace font name
     */
    String getMonoFontName() {
        // Try common monospace fonts
        String[] monoFonts = {"JetBrains Mono", "Fira Code", "Menlo", "Consolas", "Monaco", "Courier New", "Monospace"};

        // For now, return platform-appropriate default
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("mac")) {
            return "Menlo";
        } else if (os.contains("win")) {
            return "Consolas";
        } else {
            return "Monospace";
        }
    }
}
