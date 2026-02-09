/*
 * Copyright (c) 2024 Example
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.codepilot1c.ui.chat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;

import com.codepilot1c.core.agent.AgentState;

/**
 * Виджет для отображения прогресса выполнения агента.
 *
 * <p>Показывает:</p>
 * <ul>
 *   <li>Индикатор прогресса (шаги)</li>
 *   <li>Текущий статус</li>
 *   <li>Состояние агента</li>
 * </ul>
 */
public class AgentProgressWidget extends Composite {

    private ProgressBar progressBar;
    private Label statusLabel;
    private Label stateLabel;
    private Label stepLabel;

    /**
     * Создаёт виджет прогресса.
     *
     * @param parent родительский composite
     */
    public AgentProgressWidget(Composite parent) {
        super(parent, SWT.NONE);
        createContents();
    }

    private void createContents() {
        GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = 8;
        layout.marginHeight = 4;
        layout.horizontalSpacing = 8;
        setLayout(layout);

        // State indicator
        stateLabel = new Label(this, SWT.NONE);
        stateLabel.setText("◯ Ожидание"); //$NON-NLS-1$
        stateLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

        // Step counter
        stepLabel = new Label(this, SWT.NONE);
        stepLabel.setText(""); //$NON-NLS-1$
        stepLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));

        // Progress bar
        progressBar = new ProgressBar(this, SWT.HORIZONTAL | SWT.SMOOTH);
        GridData progressData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        progressData.horizontalSpan = 2;
        progressBar.setLayoutData(progressData);
        progressBar.setMinimum(0);
        progressBar.setMaximum(100);
        progressBar.setSelection(0);

        // Status label
        statusLabel = new Label(this, SWT.WRAP);
        GridData statusData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        statusData.horizontalSpan = 2;
        statusLabel.setLayoutData(statusData);
        statusLabel.setText(""); //$NON-NLS-1$

        // Initially hidden
        setVisible(false);
        GridData myData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        myData.exclude = true;
        setLayoutData(myData);
    }

    /**
     * Обновляет прогресс.
     *
     * @param current текущий шаг
     * @param max максимальное количество шагов
     * @param status текстовый статус
     */
    public void updateProgress(int current, int max, String status) {
        if (isDisposed()) return;

        // Update progress bar
        int percentage = max > 0 ? (current * 100) / max : 0;
        progressBar.setSelection(percentage);

        // Update step counter
        stepLabel.setText(String.format("Шаг %d/%d", current, max)); //$NON-NLS-1$

        // Update status
        if (status != null) {
            statusLabel.setText(status);
        }

        // Make sure we're visible
        showWidget();
    }

    /**
     * Обновляет состояние агента.
     *
     * @param state новое состояние
     * @param errorMessage сообщение об ошибке (может быть null)
     */
    public void updateState(AgentState state, String errorMessage) {
        if (isDisposed()) return;

        String stateText;
        switch (state) {
            case IDLE:
                stateText = "◯ Ожидание"; //$NON-NLS-1$
                hideWidget();
                return;
            case RUNNING:
                stateText = "▶ Выполнение"; //$NON-NLS-1$
                break;
            case WAITING_TOOL:
                stateText = "🔧 Инструмент"; //$NON-NLS-1$
                break;
            case WAITING_CONFIRMATION:
                stateText = "⏸ Подтверждение"; //$NON-NLS-1$
                break;
            case COMPLETED:
                stateText = "✓ Завершено"; //$NON-NLS-1$
                break;
            case CANCELLED:
                stateText = "⊘ Отменено"; //$NON-NLS-1$
                break;
            case ERROR:
                stateText = "✗ Ошибка"; //$NON-NLS-1$
                if (errorMessage != null) {
                    statusLabel.setText(errorMessage);
                }
                break;
            default:
                stateText = state.toString();
        }

        stateLabel.setText(stateText);
        showWidget();

        // For terminal states, hide after a delay
        if (state == AgentState.COMPLETED || state == AgentState.CANCELLED) {
            getDisplay().timerExec(3000, () -> {
                if (!isDisposed() && (stateLabel.getText().equals("✓ Завершено") ||
                        stateLabel.getText().equals("⊘ Отменено"))) {
                    hideWidget();
                }
            });
        }
    }

    /**
     * Устанавливает режим неопределённого прогресса.
     * Примечание: SWT не поддерживает динамическое изменение стиля,
     * поэтому этот метод не имеет эффекта.
     */
    public void setIndeterminate(boolean indeterminate) {
        // SWT ProgressBar style cannot be changed after creation
        // This method is kept for API compatibility
    }

    /**
     * Сбрасывает виджет в начальное состояние.
     */
    public void reset() {
        if (isDisposed()) return;

        progressBar.setSelection(0);
        stateLabel.setText("◯ Ожидание"); //$NON-NLS-1$
        stepLabel.setText(""); //$NON-NLS-1$
        statusLabel.setText(""); //$NON-NLS-1$
        hideWidget();
    }

    private void showWidget() {
        if (isDisposed()) return;

        if (!getVisible()) {
            setVisible(true);
            GridData data = (GridData) getLayoutData();
            if (data != null) {
                data.exclude = false;
            }
            getParent().layout(true, true);
        }
    }

    private void hideWidget() {
        if (isDisposed()) return;

        if (getVisible()) {
            setVisible(false);
            GridData data = (GridData) getLayoutData();
            if (data != null) {
                data.exclude = true;
            }
            getParent().layout(true, true);
        }
    }
}
