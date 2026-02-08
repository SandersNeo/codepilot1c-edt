/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
 */
package com.codepilot1c.ui.preferences;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;

import com.codepilot1c.core.mcp.config.McpServerConfig;
import com.codepilot1c.ui.internal.Messages;

/**
 * Dialog for editing MCP server configuration.
 */
public class McpServerEditDialog extends TitleAreaDialog {

    private McpServerConfig existingConfig;
    private Text nameText;
    private Button enabledCheckbox;
    private Text commandText;
    private Text argsText;
    private List<Map.Entry<String, String>> envVariables = new ArrayList<>();
    private TableViewer envTableViewer;
    private Text workingDirText;
    private Spinner connectionTimeoutSpinner;
    private Spinner requestTimeoutSpinner;

    // Saved config (widgets are disposed after dialog closes)
    private McpServerConfig savedConfig;

    /**
     * Creates a new dialog.
     *
     * @param parentShell the parent shell
     * @param existingConfig existing config to edit, or null for new
     */
    public McpServerEditDialog(Shell parentShell, McpServerConfig existingConfig) {
        super(parentShell);
        this.existingConfig = existingConfig;
        if (existingConfig != null) {
            existingConfig.getEnv().forEach((k, v) ->
                envVariables.add(new AbstractMap.SimpleEntry<>(k, v)));
        }
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText(existingConfig == null ?
            Messages.McpServerEditDialog_TitleAdd :
            Messages.McpServerEditDialog_TitleEdit);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        setTitle(existingConfig == null ?
            Messages.McpServerEditDialog_TitleAdd :
            Messages.McpServerEditDialog_TitleEdit);
        setMessage(Messages.McpServerEditDialog_Description);

        Composite area = (Composite) super.createDialogArea(parent);
        Composite container = new Composite(area, SWT.NONE);
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        container.setLayout(new GridLayout(2, false));

        // Name
        createLabel(container, Messages.McpServerEditDialog_Name);
        nameText = new Text(container, SWT.BORDER);
        nameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        if (existingConfig != null) {
            nameText.setText(existingConfig.getName());
        }

        // Enabled
        enabledCheckbox = new Button(container, SWT.CHECK);
        enabledCheckbox.setText(Messages.McpServerEditDialog_Enabled);
        enabledCheckbox.setSelection(existingConfig == null || existingConfig.isEnabled());
        GridData gd = new GridData();
        gd.horizontalSpan = 2;
        enabledCheckbox.setLayoutData(gd);

        // Command
        createLabel(container, Messages.McpServerEditDialog_Command);
        commandText = new Text(container, SWT.BORDER);
        commandText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        commandText.setMessage("npx, node, python...");
        if (existingConfig != null) {
            commandText.setText(existingConfig.getCommand());
        }

        // Arguments
        createLabel(container, Messages.McpServerEditDialog_Arguments);
        argsText = new Text(container, SWT.BORDER);
        argsText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        argsText.setMessage("-y @modelcontextprotocol/server-filesystem /path");
        if (existingConfig != null && !existingConfig.getArgs().isEmpty()) {
            argsText.setText(String.join(" ", existingConfig.getArgs()));
        }

        // Working directory
        createLabel(container, Messages.McpServerEditDialog_WorkingDirectory);
        Composite dirComposite = new Composite(container, SWT.NONE);
        dirComposite.setLayout(new GridLayout(2, false));
        dirComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        workingDirText = new Text(dirComposite, SWT.BORDER);
        workingDirText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        if (existingConfig != null && existingConfig.getWorkingDirectory() != null) {
            workingDirText.setText(existingConfig.getWorkingDirectory());
        }
        Button browseButton = new Button(dirComposite, SWT.PUSH);
        browseButton.setText(Messages.McpServerEditDialog_Browse);
        browseButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> browseDirectory()));

        // Environment variables
        createLabel(container, Messages.McpServerEditDialog_Environment);
        createEnvTable(container);

        // Timeouts
        createLabel(container, Messages.McpServerEditDialog_ConnectionTimeout);
        connectionTimeoutSpinner = new Spinner(container, SWT.BORDER);
        connectionTimeoutSpinner.setMinimum(5);
        connectionTimeoutSpinner.setMaximum(120);
        connectionTimeoutSpinner.setSelection(existingConfig != null ?
            existingConfig.getConnectionTimeoutMs() / 1000 : 30);

        createLabel(container, Messages.McpServerEditDialog_RequestTimeout);
        requestTimeoutSpinner = new Spinner(container, SWT.BORDER);
        requestTimeoutSpinner.setMinimum(10);
        requestTimeoutSpinner.setMaximum(300);
        requestTimeoutSpinner.setSelection(existingConfig != null ?
            existingConfig.getRequestTimeoutMs() / 1000 : 60);

        return area;
    }

    private void createLabel(Composite parent, String text) {
        Label label = new Label(parent, SWT.NONE);
        label.setText(text);
    }

    private void createEnvTable(Composite parent) {
        Composite envComposite = new Composite(parent, SWT.NONE);
        envComposite.setLayout(new GridLayout(2, false));
        envComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        envTableViewer = new TableViewer(envComposite, SWT.BORDER | SWT.FULL_SELECTION);
        Table table = envTableViewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        gd.heightHint = 80;
        table.setLayoutData(gd);

        TableColumn keyCol = new TableColumn(table, SWT.NONE);
        keyCol.setText(Messages.McpServerEditDialog_EnvKey);
        keyCol.setWidth(120);

        TableColumn valueCol = new TableColumn(table, SWT.NONE);
        valueCol.setText(Messages.McpServerEditDialog_EnvValue);
        valueCol.setWidth(180);

        envTableViewer.setContentProvider(ArrayContentProvider.getInstance());
        envTableViewer.setLabelProvider(new EnvLabelProvider());
        envTableViewer.setInput(envVariables);

        // Buttons
        Composite envButtons = new Composite(envComposite, SWT.NONE);
        envButtons.setLayout(new GridLayout(1, false));
        envButtons.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));

        Button addEnvButton = new Button(envButtons, SWT.PUSH);
        addEnvButton.setText(Messages.McpServerEditDialog_AddEnv);
        addEnvButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        addEnvButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> addEnvVariable()));

        Button removeEnvButton = new Button(envButtons, SWT.PUSH);
        removeEnvButton.setText(Messages.McpServerEditDialog_RemoveEnv);
        removeEnvButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        removeEnvButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> removeEnvVariable()));
    }

    private void addEnvVariable() {
        InputDialog keyDialog = new InputDialog(getShell(),
            Messages.McpServerEditDialog_EnvKey,
            Messages.McpServerEditDialog_EnvKeyPrompt,
            "", null);
        if (keyDialog.open() == Window.OK) {
            String key = keyDialog.getValue();
            InputDialog valueDialog = new InputDialog(getShell(),
                Messages.McpServerEditDialog_EnvValue,
                Messages.McpServerEditDialog_EnvValuePrompt,
                "", null);
            if (valueDialog.open() == Window.OK) {
                envVariables.add(new AbstractMap.SimpleEntry<>(key, valueDialog.getValue()));
                envTableViewer.refresh();
            }
        }
    }

    private void removeEnvVariable() {
        IStructuredSelection selection = envTableViewer.getStructuredSelection();
        if (!selection.isEmpty()) {
            envVariables.remove(selection.getFirstElement());
            envTableViewer.refresh();
        }
    }

    private void browseDirectory() {
        DirectoryDialog dialog = new DirectoryDialog(getShell());
        dialog.setText(Messages.McpServerEditDialog_SelectDirectory);
        String path = dialog.open();
        if (path != null) {
            workingDirText.setText(path);
        }
    }

    @Override
    protected void okPressed() {
        if (!validateInput()) {
            return;
        }
        // Save config BEFORE dialog closes (widgets will be disposed after super.okPressed())
        savedConfig = buildServerConfig();
        super.okPressed();
    }

    private boolean validateInput() {
        if (nameText.getText().trim().isEmpty()) {
            setErrorMessage(Messages.McpServerEditDialog_NameRequired);
            nameText.setFocus();
            return false;
        }
        if (commandText.getText().trim().isEmpty()) {
            setErrorMessage(Messages.McpServerEditDialog_CommandRequired);
            commandText.setFocus();
            return false;
        }
        setErrorMessage(null);
        return true;
    }

    /**
     * Returns the configured server.
     * Must be called after dialog.open() returns Window.OK.
     *
     * @return the server configuration
     */
    public McpServerConfig getServerConfig() {
        return savedConfig;
    }

    /**
     * Builds server config from current widget values.
     * Must be called while dialog is still open (before widgets are disposed).
     */
    private McpServerConfig buildServerConfig() {
        McpServerConfig.Builder builder = McpServerConfig.builder()
            .name(nameText.getText().trim())
            .enabled(enabledCheckbox.getSelection())
            .command(commandText.getText().trim())
            .args(parseArgs(argsText.getText()))
            .connectionTimeout(connectionTimeoutSpinner.getSelection() * 1000)
            .requestTimeout(requestTimeoutSpinner.getSelection() * 1000);

        if (existingConfig != null) {
            builder.id(existingConfig.getId());
        }

        String workDir = workingDirText.getText().trim();
        if (!workDir.isEmpty()) {
            builder.workingDirectory(workDir);
        }

        for (Map.Entry<String, String> entry : envVariables) {
            builder.putEnv(entry.getKey(), entry.getValue());
        }

        return builder.build();
    }

    private List<String> parseArgs(String argsString) {
        List<String> args = new ArrayList<>();
        if (argsString == null || argsString.trim().isEmpty()) {
            return args;
        }

        // Shell-like argument parsing with quote support
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        char quoteChar = 0;

        for (char c : argsString.toCharArray()) {
            if ((c == '"' || c == '\'') && !inQuotes) {
                inQuotes = true;
                quoteChar = c;
            } else if (c == quoteChar && inQuotes) {
                inQuotes = false;
                quoteChar = 0;
            } else if (c == ' ' && !inQuotes) {
                if (current.length() > 0) {
                    args.add(current.toString());
                    current = new StringBuilder();
                }
            } else {
                current.append(c);
            }
        }

        if (current.length() > 0) {
            args.add(current.toString());
        }

        return args;
    }

    /**
     * Label provider for environment variables table.
     */
    private static class EnvLabelProvider extends LabelProvider implements ITableLabelProvider {

        @Override
        public String getColumnText(Object element, int columnIndex) {
            @SuppressWarnings("unchecked")
            Map.Entry<String, String> entry = (Map.Entry<String, String>) element;
            return columnIndex == 0 ? entry.getKey() : entry.getValue();
        }

        @Override
        public Image getColumnImage(Object element, int columnIndex) {
            return null;
        }
    }
}
