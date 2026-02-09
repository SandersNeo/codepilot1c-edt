/*
 * Copyright (c) 2024 Example
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.codepilot1c.ui.preferences;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

import com.codepilot1c.ui.internal.Messages;

/**
 * A composite editor for managing custom models with active model selection.
 *
 * <p>Features:
 * <ul>
 *   <li>List of custom models</li>
 *   <li>Add/Edit/Remove buttons</li>
 *   <li>Combo box to select active model</li>
 * </ul>
 */
public class ModelConfigurationEditor {

    private static final String SEPARATOR = ";"; //$NON-NLS-1$

    private final String modelsPreferenceName;
    private final String activeModelPreferenceName;
    private final IPreferenceStore preferenceStore;

    private Composite container;
    private org.eclipse.swt.widgets.List modelList;
    private Combo activeModelCombo;
    private Button addButton;
    private Button editButton;
    private Button removeButton;

    private List<String> models = new ArrayList<>();
    private String activeModel = ""; //$NON-NLS-1$

    /**
     * Creates a model configuration editor.
     *
     * @param parent the parent composite
     * @param title the group title
     * @param modelsPreferenceName the preference name for the models list
     * @param activeModelPreferenceName the preference name for the active model
     * @param preferenceStore the preference store
     */
    public ModelConfigurationEditor(Composite parent, String title,
                                     String modelsPreferenceName, String activeModelPreferenceName,
                                     IPreferenceStore preferenceStore) {
        this.modelsPreferenceName = modelsPreferenceName;
        this.activeModelPreferenceName = activeModelPreferenceName;
        this.preferenceStore = preferenceStore;

        createControl(parent, title);
        load();
    }

    private void createControl(Composite parent, String title) {
        Group group = new Group(parent, SWT.NONE);
        group.setText(title);
        group.setLayout(new GridLayout(2, false));
        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
        gd.horizontalSpan = 2;
        group.setLayoutData(gd);

        container = group;

        // Active model selection
        Label activeLabel = new Label(group, SWT.NONE);
        activeLabel.setText(Messages.ModelConfigurationEditor_ActiveModelLabel);

        activeModelCombo = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
        activeModelCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        activeModelCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int index = activeModelCombo.getSelectionIndex();
                if (index >= 0 && index < models.size()) {
                    activeModel = models.get(index);
                }
            }
        });

        // Models label
        Label modelsLabel = new Label(group, SWT.NONE);
        modelsLabel.setText(Messages.ModelConfigurationEditor_CustomModelsLabel);
        gd = new GridData();
        gd.horizontalSpan = 2;
        modelsLabel.setLayoutData(gd);

        // List and buttons container
        Composite listContainer = new Composite(group, SWT.NONE);
        listContainer.setLayout(new GridLayout(2, false));
        gd = new GridData(SWT.FILL, SWT.FILL, true, false);
        gd.horizontalSpan = 2;
        listContainer.setLayoutData(gd);

        // Models list
        modelList = new org.eclipse.swt.widgets.List(listContainer, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL);
        gd = new GridData(SWT.FILL, SWT.FILL, true, false);
        gd.heightHint = 80;
        modelList.setLayoutData(gd);
        modelList.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateButtons();
            }
        });

        // Buttons
        Composite buttonBox = new Composite(listContainer, SWT.NONE);
        buttonBox.setLayout(new GridLayout(1, true));
        buttonBox.setLayoutData(new GridData(SWT.CENTER, SWT.BEGINNING, false, false));

        addButton = new Button(buttonBox, SWT.PUSH);
        addButton.setText(Messages.ModelListFieldEditor_AddButton);
        addButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        addButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                addModel();
            }
        });

        editButton = new Button(buttonBox, SWT.PUSH);
        editButton.setText(Messages.ModelListFieldEditor_EditButton);
        editButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        editButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                editModel();
            }
        });

        removeButton = new Button(buttonBox, SWT.PUSH);
        removeButton.setText(Messages.ModelListFieldEditor_RemoveButton);
        removeButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        removeButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                removeModel();
            }
        });

        updateButtons();
    }

    /**
     * Loads values from the preference store.
     */
    public void load() {
        String modelsValue = preferenceStore.getString(modelsPreferenceName);
        activeModel = preferenceStore.getString(activeModelPreferenceName);
        parseModels(modelsValue);
        populateControls();
    }

    /**
     * Loads default values from the preference store.
     */
    public void loadDefaults() {
        String modelsValue = preferenceStore.getDefaultString(modelsPreferenceName);
        activeModel = preferenceStore.getDefaultString(activeModelPreferenceName);
        parseModels(modelsValue);
        populateControls();
    }

    /**
     * Stores values to the preference store.
     */
    public void store() {
        String modelsValue = String.join(SEPARATOR, models);
        preferenceStore.setValue(modelsPreferenceName, modelsValue);
        preferenceStore.setValue(activeModelPreferenceName, activeModel);
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

    private void populateControls() {
        // Populate list
        modelList.removeAll();
        for (String model : models) {
            modelList.add(model);
        }

        // Populate combo
        activeModelCombo.removeAll();
        int activeIndex = -1;
        for (int i = 0; i < models.size(); i++) {
            String model = models.get(i);
            activeModelCombo.add(model);
            if (model.equals(activeModel)) {
                activeIndex = i;
            }
        }

        // Select active model
        if (activeIndex >= 0) {
            activeModelCombo.select(activeIndex);
        } else if (!models.isEmpty()) {
            activeModelCombo.select(0);
            activeModel = models.get(0);
        }

        updateButtons();
    }

    private void updateButtons() {
        int index = modelList.getSelectionIndex();
        editButton.setEnabled(index >= 0);
        removeButton.setEnabled(index >= 0);
    }

    private void addModel() {
        InputDialog dialog = new InputDialog(
                container.getShell(),
                Messages.ModelListFieldEditor_AddDialogTitle,
                Messages.ModelListFieldEditor_AddDialogMessage,
                "", //$NON-NLS-1$
                new ModelNameValidator()
        );

        if (dialog.open() == Window.OK) {
            String newModel = dialog.getValue().trim();
            if (!newModel.isEmpty() && !models.contains(newModel)) {
                models.add(newModel);
                populateControls();
                modelList.setSelection(models.size() - 1);

                // If this is the first model, make it active
                if (models.size() == 1) {
                    activeModel = newModel;
                    activeModelCombo.select(0);
                }

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
        boolean wasActive = currentModel.equals(activeModel);

        InputDialog dialog = new InputDialog(
                container.getShell(),
                Messages.ModelListFieldEditor_EditDialogTitle,
                Messages.ModelListFieldEditor_EditDialogMessage,
                currentModel,
                new ModelNameValidator()
        );

        if (dialog.open() == Window.OK) {
            String newModel = dialog.getValue().trim();
            if (!newModel.isEmpty()) {
                models.set(index, newModel);
                if (wasActive) {
                    activeModel = newModel;
                }
                populateControls();
                modelList.setSelection(index);
                updateButtons();
            }
        }
    }

    private void removeModel() {
        int index = modelList.getSelectionIndex();
        if (index >= 0) {
            String removedModel = models.remove(index);

            // If removing active model, select another
            if (removedModel.equals(activeModel)) {
                if (!models.isEmpty()) {
                    activeModel = models.get(0);
                } else {
                    activeModel = ""; //$NON-NLS-1$
                }
            }

            populateControls();
            if (index < models.size()) {
                modelList.setSelection(index);
            } else if (!models.isEmpty()) {
                modelList.setSelection(models.size() - 1);
            }
            updateButtons();
        }
    }

    /**
     * Returns the container composite.
     *
     * @return the container
     */
    public Composite getControl() {
        return container;
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
     * Returns the active model.
     *
     * @return the active model name
     */
    public String getActiveModel() {
        return activeModel;
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
