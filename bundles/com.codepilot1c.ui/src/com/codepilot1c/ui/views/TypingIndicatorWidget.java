/*
 * Copyright (c) 2024 Example
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.codepilot1c.ui.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.codepilot1c.ui.theme.ThemeManager;
import com.codepilot1c.ui.theme.VibeTheme;

/**
 * Modern typing indicator widget with animated dots.
 *
 * <p>Shows animated bouncing dots similar to modern chat apps.</p>
 */
public class TypingIndicatorWidget extends Composite {

    private static final int ANIMATION_INTERVAL_MS = 150;
    private static final int DOT_COUNT = 3;
    private static final int DOT_SIZE = 6;
    private static final int DOT_SPACING = 4;

    private Canvas dotsCanvas;
    private Label textLabel;
    private Runnable animationRunnable;
    private int animationFrame = 0;
    private boolean isAnimating = false;
    private final VibeTheme theme;

    /**
     * Creates the typing indicator widget.
     *
     * @param parent the parent composite
     */
    public TypingIndicatorWidget(Composite parent) {
        super(parent, SWT.NONE);
        this.theme = ThemeManager.getInstance().getTheme();
        createContents();
        createAnimationRunnable();
    }

    private void createContents() {
        setBackground(theme.getAssistantMessageBackground());

        GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = 12;
        layout.marginHeight = 8;
        layout.horizontalSpacing = 8;
        setLayout(layout);

        // Animated dots canvas
        dotsCanvas = new Canvas(this, SWT.DOUBLE_BUFFERED);
        dotsCanvas.setBackground(getBackground());
        GridData dotsData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
        dotsData.widthHint = DOT_COUNT * DOT_SIZE + (DOT_COUNT - 1) * DOT_SPACING;
        dotsData.heightHint = DOT_SIZE + 8; // Extra height for bounce animation
        dotsCanvas.setLayoutData(dotsData);
        dotsCanvas.addPaintListener(this::paintDots);

        // Text label
        textLabel = new Label(this, SWT.NONE);
        textLabel.setText("AI обрабатывает запрос"); //$NON-NLS-1$
        textLabel.setBackground(getBackground());
        textLabel.setForeground(theme.getTextMuted());
        textLabel.setFont(theme.getFont());
        textLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));

        // Initially hidden
        setVisible(false);
        GridData myData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        myData.exclude = true;
        setLayoutData(myData);
    }

    private void paintDots(PaintEvent e) {
        GC gc = e.gc;
        gc.setAntialias(SWT.ON);

        Point size = dotsCanvas.getSize();
        int baseY = size.y / 2;

        for (int i = 0; i < DOT_COUNT; i++) {
            // Calculate bounce offset based on animation frame
            int frameOffset = (animationFrame - i + DOT_COUNT * 2) % (DOT_COUNT * 2);
            int bounceY = 0;

            if (frameOffset < DOT_COUNT) {
                // Bouncing up phase
                bounceY = -frameOffset * 2;
                if (frameOffset > 1) bounceY = -(DOT_COUNT - frameOffset) * 2;
            }

            int x = i * (DOT_SIZE + DOT_SPACING);
            int y = baseY + bounceY - DOT_SIZE / 2;

            // Draw dot with gradient effect
            gc.setBackground(theme.getAccent());
            gc.fillOval(x, y, DOT_SIZE, DOT_SIZE);
        }
    }

    private void createAnimationRunnable() {
        animationRunnable = () -> {
            if (isDisposed() || !isAnimating) {
                return;
            }

            animationFrame = (animationFrame + 1) % (DOT_COUNT * 2);

            if (!dotsCanvas.isDisposed()) {
                dotsCanvas.redraw();
            }

            // Schedule next frame
            if (isAnimating && !isDisposed()) {
                getDisplay().timerExec(ANIMATION_INTERVAL_MS, animationRunnable);
            }
        };
    }

    /**
     * Shows the indicator and starts the animation.
     */
    public void show() {
        if (isDisposed()) {
            return;
        }

        // Always move to bottom of parent
        moveBelow(null);

        setVisible(true);
        GridData data = (GridData) getLayoutData();
        if (data != null) {
            data.exclude = false;
        }

        // Start animation
        if (!isAnimating) {
            isAnimating = true;
            animationFrame = 0;
            getDisplay().timerExec(ANIMATION_INTERVAL_MS, animationRunnable);
        }

        // Request layout update
        getParent().layout(true, true);
    }

    /**
     * Hides the indicator and stops the animation.
     */
    public void hide() {
        if (isDisposed()) {
            return;
        }

        // Stop animation
        isAnimating = false;

        setVisible(false);
        GridData data = (GridData) getLayoutData();
        if (data != null) {
            data.exclude = true;
        }

        // Request layout update
        getParent().layout(true, true);
    }

    /**
     * Sets the indicator text.
     *
     * @param text the text to display
     */
    public void setText(String text) {
        if (!textLabel.isDisposed()) {
            textLabel.setText(text);
        }
    }

    /**
     * Checks if the indicator is currently showing.
     *
     * @return true if showing
     */
    public boolean isShowing() {
        return getVisible() && isAnimating;
    }

    @Override
    public void dispose() {
        isAnimating = false;
        super.dispose();
    }
}
