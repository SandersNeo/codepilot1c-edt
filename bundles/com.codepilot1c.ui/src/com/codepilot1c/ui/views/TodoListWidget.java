/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
 */
package com.codepilot1c.ui.views;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;

import com.codepilot1c.ui.chat.MessagePart.TodoListPart;
import com.codepilot1c.ui.chat.MessagePart.TodoListPart.TodoItem;
import com.codepilot1c.ui.theme.ThemeManager;
import com.codepilot1c.ui.theme.VibeTheme;

/**
 * Widget for displaying a todo/task list with checkboxes and progress.
 */
public class TodoListWidget extends Composite {

    private final List<TodoItem> items;
    private final List<Button> checkboxes = new ArrayList<>();
    private final VibeTheme theme;

    private Label progressLabel;
    private ProgressBar progressBar;

    /**
     * Creates a new todo list widget.
     *
     * @param parent the parent composite
     * @param todoList the todo list part
     */
    public TodoListWidget(Composite parent, TodoListPart todoList) {
        super(parent, SWT.NONE);
        this.items = new ArrayList<>(todoList.items());
        this.theme = ThemeManager.getInstance().getTheme();

        createContents();
    }

    /**
     * Creates a new todo list widget.
     *
     * @param parent the parent composite
     * @param items the todo items
     */
    public TodoListWidget(Composite parent, List<TodoItem> items) {
        super(parent, SWT.NONE);
        this.items = new ArrayList<>(items);
        this.theme = ThemeManager.getInstance().getTheme();

        createContents();
    }

    private void createContents() {
        // Layout
        GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = theme.getMargin();
        layout.marginHeight = theme.getMargin();
        layout.verticalSpacing = theme.getMarginSmall();
        setLayout(layout);
        setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        setBackground(theme.getSurfaceElevated());

        // Header with progress
        createHeader();

        // Todo items
        createItems();
    }

    private void createHeader() {
        Composite header = new Composite(this, SWT.NONE);
        header.setBackground(getBackground());
        GridLayout headerLayout = new GridLayout(3, false);
        headerLayout.marginWidth = 0;
        headerLayout.marginHeight = 0;
        header.setLayout(headerLayout);
        header.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        // Title
        Label titleLabel = new Label(header, SWT.NONE);
        titleLabel.setBackground(header.getBackground());
        titleLabel.setForeground(theme.getText());
        titleLabel.setFont(theme.getFontBold());
        titleLabel.setText("\u2611 Задачи"); // ☑ //$NON-NLS-1$
        titleLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

        // Progress bar
        progressBar = new ProgressBar(header, SWT.HORIZONTAL);
        progressBar.setMinimum(0);
        progressBar.setMaximum(100);
        GridData progressData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        progressData.widthHint = 100;
        progressBar.setLayoutData(progressData);

        // Progress label
        progressLabel = new Label(header, SWT.NONE);
        progressLabel.setBackground(header.getBackground());
        progressLabel.setForeground(theme.getTextMuted());
        progressLabel.setFont(theme.getFontSmall());
        progressLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

        updateProgress();
    }

    private void createItems() {
        Composite itemsContainer = new Composite(this, SWT.NONE);
        itemsContainer.setBackground(getBackground());
        GridLayout itemsLayout = new GridLayout(1, false);
        itemsLayout.marginWidth = theme.getMargin();
        itemsLayout.marginHeight = 0;
        itemsLayout.verticalSpacing = 2;
        itemsContainer.setLayout(itemsLayout);
        itemsContainer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        for (int i = 0; i < items.size(); i++) {
            TodoItem item = items.get(i);
            final int index = i;

            Composite itemRow = new Composite(itemsContainer, SWT.NONE);
            itemRow.setBackground(itemsContainer.getBackground());
            itemRow.setLayout(new GridLayout(2, false));
            itemRow.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

            // Checkbox
            Button checkbox = new Button(itemRow, SWT.CHECK);
            checkbox.setBackground(itemRow.getBackground());
            checkbox.setSelection(item.checked());
            checkbox.addListener(SWT.Selection, e -> {
                // Update the item
                TodoItem oldItem = items.get(index);
                items.set(index, new TodoItem(oldItem.text(), checkbox.getSelection()));
                updateProgress();
            });
            checkboxes.add(checkbox);

            // Label
            Label itemLabel = new Label(itemRow, SWT.WRAP);
            itemLabel.setBackground(itemRow.getBackground());
            itemLabel.setForeground(theme.getText());
            itemLabel.setFont(theme.getFont());
            itemLabel.setText(item.text());
            GridData labelData = new GridData(SWT.FILL, SWT.CENTER, true, false);
            labelData.widthHint = 300;
            itemLabel.setLayoutData(labelData);

            // Strike through if checked
            if (item.checked()) {
                // Note: SWT doesn't support strikethrough directly on Label
                // Could use StyledText instead for full formatting
            }
        }
    }

    private void updateProgress() {
        if (items.isEmpty()) {
            progressBar.setSelection(0);
            progressLabel.setText("0/0"); //$NON-NLS-1$
            return;
        }

        int completed = (int) items.stream().filter(TodoItem::checked).count();
        int total = items.size();
        int percent = (completed * 100) / total;

        progressBar.setSelection(percent);
        progressLabel.setText(completed + "/" + total); //$NON-NLS-1$
    }

    /**
     * Returns the current items.
     *
     * @return the todo items
     */
    public List<TodoItem> getItems() {
        return new ArrayList<>(items);
    }

    /**
     * Returns the completion percentage.
     *
     * @return 0-100 percentage
     */
    public int getCompletionPercent() {
        if (items.isEmpty()) {
            return 0;
        }
        int completed = (int) items.stream().filter(TodoItem::checked).count();
        return (completed * 100) / items.size();
    }

    @Override
    public void dispose() {
        checkboxes.clear();
        // All colors and fonts are managed by ThemeManager - no local disposal needed
        super.dispose();
    }
}
