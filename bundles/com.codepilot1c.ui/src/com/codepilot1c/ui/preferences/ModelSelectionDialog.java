/*
 * Copyright (c) 2024 Example
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.codepilot1c.ui.preferences;

import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.codepilot1c.core.provider.config.ModelFetchService.ModelInfo;
import com.codepilot1c.ui.internal.Messages;

/**
 * Dialog for selecting a model from a list of available models.
 */
public class ModelSelectionDialog extends Dialog {

    private final List<ModelInfo> models;
    private ModelInfo selectedModel;
    private Text filterText;
    private TableViewer tableViewer;
    private String filterString = ""; //$NON-NLS-1$

    public ModelSelectionDialog(Shell parentShell, List<ModelInfo> models) {
        super(parentShell);
        this.models = models;
        setShellStyle(getShellStyle() | SWT.RESIZE);
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText(Messages.ModelSelectionDialog_Title);
        shell.setMinimumSize(400, 400);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);

        Composite container = new Composite(area, SWT.NONE);
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        container.setLayout(new GridLayout(1, false));

        // Filter field
        Label filterLabel = new Label(container, SWT.NONE);
        filterLabel.setText(Messages.ModelSelectionDialog_Filter);

        filterText = new Text(container, SWT.BORDER | SWT.SEARCH | SWT.ICON_SEARCH);
        filterText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        filterText.setMessage(Messages.ModelSelectionDialog_FilterPlaceholder);
        filterText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                filterString = filterText.getText().toLowerCase();
                tableViewer.refresh();
            }
        });

        // Model count label
        Label countLabel = new Label(container, SWT.NONE);
        countLabel.setText(String.format(Messages.ModelSelectionDialog_ModelCount, models.size()));
        countLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        // Table
        tableViewer = new TableViewer(container, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION | SWT.V_SCROLL);
        tableViewer.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        tableViewer.setContentProvider(ArrayContentProvider.getInstance());
        tableViewer.setLabelProvider(new LabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof ModelInfo) {
                    return ((ModelInfo) element).toString();
                }
                return super.getText(element);
            }
        });

        // Filter
        tableViewer.addFilter(new ViewerFilter() {
            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element) {
                if (filterString.isEmpty()) {
                    return true;
                }
                if (element instanceof ModelInfo) {
                    ModelInfo model = (ModelInfo) element;
                    return model.getId().toLowerCase().contains(filterString) ||
                            (model.getName() != null && model.getName().toLowerCase().contains(filterString));
                }
                return true;
            }
        });

        tableViewer.setInput(models);

        // Selection listener
        tableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                IStructuredSelection selection = (IStructuredSelection) event.getSelection();
                selectedModel = (ModelInfo) selection.getFirstElement();
                updateOkButton();
            }
        });

        // Double-click to select
        tableViewer.addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(DoubleClickEvent event) {
                IStructuredSelection selection = (IStructuredSelection) event.getSelection();
                selectedModel = (ModelInfo) selection.getFirstElement();
                if (selectedModel != null) {
                    okPressed();
                }
            }
        });

        return area;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        super.createButtonsForButtonBar(parent);
        updateOkButton();
    }

    private void updateOkButton() {
        getButton(IDialogConstants.OK_ID).setEnabled(selectedModel != null);
    }

    /**
     * Returns the selected model.
     */
    public ModelInfo getSelectedModel() {
        return selectedModel;
    }
}
