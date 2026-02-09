/*
 * Copyright (c) 2024 Example
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.codepilot1c.ui.preferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.codepilot1c.ui.internal.Messages;

/**
 * A field editor for managing a list of custom models.
 * Models are stored as a semicolon-separated string.
 */
public class ModelListFieldEditor extends FieldEditor {

    private static final String SEPARATOR = ";"; //$NON-NLS-1$

    private org.eclipse.swt.widgets.List modelList;
    private Button addButton;
    private Button editButton;
    private Button removeButton;
    private Button upButton;
    private Button downButton;
    private Composite buttonBox;

    private List<String> models = new ArrayList<>();

    /**
     * Creates a model list field editor.
     *
     * @param name the preference name
     * @param labelText the label text
     * @param parent the parent composite
     */
    public ModelListFieldEditor(String name, String labelText, Composite parent) {
        init(name, labelText);
        createControl(parent);
    }

    @Override
    protected void adjustForNumColumns(int numColumns) {
        Control control = getLabelControl();
        if (control != null) {
            ((GridData) control.getLayoutData()).horizontalSpan = numColumns;
        }
        ((GridData) modelList.getLayoutData()).horizontalSpan = numColumns - 1;
    }

    @Override
    protected void doFillIntoGrid(Composite parent, int numColumns) {
        Control control = getLabelControl(parent);
        GridData gd = new GridData();
        gd.horizontalSpan = numColumns;
        control.setLayoutData(gd);

        modelList = getListControl(parent);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.verticalAlignment = GridData.FILL;
        gd.horizontalSpan = numColumns - 1;
        gd.grabExcessHorizontalSpace = true;
        gd.heightHint = 100;
        modelList.setLayoutData(gd);

        buttonBox = getButtonBoxControl(parent);
        gd = new GridData();
        gd.verticalAlignment = GridData.BEGINNING;
        buttonBox.setLayoutData(gd);
    }

    @Override
    protected void doLoad() {
        if (modelList != null) {
            String value = getPreferenceStore().getString(getPreferenceName());
            parseModels(value);
            populateList();
        }
    }

    @Override
    protected void doLoadDefault() {
        if (modelList != null) {
            String value = getPreferenceStore().getDefaultString(getPreferenceName());
            parseModels(value);
            populateList();
        }
    }

    @Override
    protected void doStore() {
        String value = String.join(SEPARATOR, models);
        getPreferenceStore().setValue(getPreferenceName(), value);
    }

    @Override
    public int getNumberOfControls() {
        return 2;
    }

    /**
     * Returns the list control.
     */
    private org.eclipse.swt.widgets.List getListControl(Composite parent) {
        if (modelList == null) {
            modelList = new org.eclipse.swt.widgets.List(parent, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL | SWT.H_SCROLL);
            modelList.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    updateButtons();
                }
            });
        }
        return modelList;
    }

    /**
     * Returns the button box control.
     */
    private Composite getButtonBoxControl(Composite parent) {
        if (buttonBox == null) {
            buttonBox = new Composite(parent, SWT.NULL);
            GridLayout layout = new GridLayout();
            layout.marginWidth = 0;
            buttonBox.setLayout(layout);

            addButton = createButton(buttonBox, Messages.ModelListFieldEditor_AddButton);
            addButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    addModel();
                }
            });

            editButton = createButton(buttonBox, Messages.ModelListFieldEditor_EditButton);
            editButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    editModel();
                }
            });

            removeButton = createButton(buttonBox, Messages.ModelListFieldEditor_RemoveButton);
            removeButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    removeModel();
                }
            });

            // Separator
            Label separator = new Label(buttonBox, SWT.NONE);
            separator.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

            upButton = createButton(buttonBox, Messages.ModelListFieldEditor_UpButton);
            upButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    moveUp();
                }
            });

            downButton = createButton(buttonBox, Messages.ModelListFieldEditor_DownButton);
            downButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    moveDown();
                }
            });

            updateButtons();
        }
        return buttonBox;
    }

    private Button createButton(Composite parent, String text) {
        Button button = new Button(parent, SWT.PUSH);
        button.setText(text);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.widthHint = 80;
        button.setLayoutData(gd);
        return button;
    }

    private void parseModels(String value) {
        models.clear();
        if (value != null && !value.isEmpty()) {
            String[] parts = value.split(SEPARATOR);
            for (String part : parts) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    models.add(trimmed);
                }
            }
        }
    }

    private void populateList() {
        modelList.removeAll();
        for (String model : models) {
            modelList.add(model);
        }
        updateButtons();
    }

    private void updateButtons() {
        int index = modelList.getSelectionIndex();
        int size = models.size();

        editButton.setEnabled(index >= 0);
        removeButton.setEnabled(index >= 0);
        upButton.setEnabled(index > 0);
        downButton.setEnabled(index >= 0 && index < size - 1);
    }

    private void addModel() {
        Shell shell = modelList.getShell();
        InputDialog dialog = new InputDialog(
                shell,
                Messages.ModelListFieldEditor_AddDialogTitle,
                Messages.ModelListFieldEditor_AddDialogMessage,
                "", //$NON-NLS-1$
                new ModelNameValidator()
        );

        if (dialog.open() == Window.OK) {
            String newModel = dialog.getValue().trim();
            if (!newModel.isEmpty() && !models.contains(newModel)) {
                models.add(newModel);
                populateList();
                modelList.setSelection(models.size() - 1);
                updateButtons();
            }
        }
    }

    private void editModel() {
        int index = modelList.getSelectionIndex();
        if (index < 0) {
            return;
        }

        String currentModel = models.get(index);
        Shell shell = modelList.getShell();
        InputDialog dialog = new InputDialog(
                shell,
                Messages.ModelListFieldEditor_EditDialogTitle,
                Messages.ModelListFieldEditor_EditDialogMessage,
                currentModel,
                new ModelNameValidator()
        );

        if (dialog.open() == Window.OK) {
            String newModel = dialog.getValue().trim();
            if (!newModel.isEmpty()) {
                models.set(index, newModel);
                populateList();
                modelList.setSelection(index);
                updateButtons();
            }
        }
    }

    private void removeModel() {
        int index = modelList.getSelectionIndex();
        if (index >= 0) {
            models.remove(index);
            populateList();
            if (index < models.size()) {
                modelList.setSelection(index);
            } else if (!models.isEmpty()) {
                modelList.setSelection(models.size() - 1);
            }
            updateButtons();
        }
    }

    private void moveUp() {
        int index = modelList.getSelectionIndex();
        if (index > 0) {
            String model = models.remove(index);
            models.add(index - 1, model);
            populateList();
            modelList.setSelection(index - 1);
            updateButtons();
        }
    }

    private void moveDown() {
        int index = modelList.getSelectionIndex();
        if (index >= 0 && index < models.size() - 1) {
            String model = models.remove(index);
            models.add(index + 1, model);
            populateList();
            modelList.setSelection(index + 1);
            updateButtons();
        }
    }

    /**
     * Returns the list of models.
     *
     * @return list of model names
     */
    public List<String> getModels() {
        return new ArrayList<>(models);
    }

    /**
     * Validator for model names.
     */
    private static class ModelNameValidator implements IInputValidator {
        @Override
        public String isValid(String newText) {
            if (newText == null || newText.trim().isEmpty()) {
                return Messages.ModelListFieldEditor_ValidationEmpty;
            }
            if (newText.contains(";")) { //$NON-NLS-1$
                return Messages.ModelListFieldEditor_ValidationSemicolon;
            }
            return null;
        }
    }
}
