/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
 */
package com.codepilot1c.ui.preferences;

import java.text.MessageFormat;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import com.codepilot1c.core.embedding.EmbeddingProviderRegistry;
import com.codepilot1c.core.embedding.EmbeddingResult;
import com.codepilot1c.core.embedding.IEmbeddingProvider;
import com.codepilot1c.core.settings.VibePreferenceConstants;
import com.codepilot1c.ui.internal.Messages;

/**
 * Preference page for configuring embedding providers and indexing settings.
 */
public class EmbeddingPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    private static final String CORE_PLUGIN_ID = "com.codepilot1c.core"; //$NON-NLS-1$

    private Button testConnectionButton;
    private Label statusLabel;

    public EmbeddingPreferencePage() {
        super(GRID);
        setDescription(Messages.EmbeddingPreferencePage_Description);
    }

    @Override
    public void init(IWorkbench workbench) {
        setPreferenceStore(new ScopedPreferenceStore(InstanceScope.INSTANCE, CORE_PLUGIN_ID));
    }

    @Override
    protected void createFieldEditors() {
        Composite parent = getFieldEditorParent();

        // Embedding provider settings
        createEmbeddingProviderGroup(parent);

        // Indexing settings
        createIndexingGroup(parent);
    }

    private void createEmbeddingProviderGroup(Composite parent) {
        Group embeddingGroup = createGroup(parent, Messages.EmbeddingPreferencePage_EmbeddingGroupTitle);

        // Provider selection - includes Auto option for zero-config experience
        String[][] providers = {
            {Messages.EmbeddingPreferencePage_ProviderAuto, VibePreferenceConstants.PREF_EMBEDDING_PROVIDER_AUTO},
            {"OpenAI", "openai"}, //$NON-NLS-1$ //$NON-NLS-2$
            {"Ollama (Local)", "ollama"} //$NON-NLS-1$ //$NON-NLS-2$
        };
        addField(new ComboFieldEditor(
                VibePreferenceConstants.PREF_EMBEDDING_PROVIDER_ID,
                Messages.EmbeddingPreferencePage_ProviderLabel,
                providers,
                embeddingGroup));

        // API URL
        addField(new StringFieldEditor(
                VibePreferenceConstants.PREF_EMBEDDING_API_URL,
                Messages.EmbeddingPreferencePage_ApiUrlLabel,
                embeddingGroup));

        // API Key (masked)
        addField(new StringFieldEditor(
                VibePreferenceConstants.PREF_EMBEDDING_API_KEY,
                Messages.EmbeddingPreferencePage_ApiKeyLabel,
                embeddingGroup) {
            @Override
            protected void doFillIntoGrid(Composite parent, int numColumns) {
                super.doFillIntoGrid(parent, numColumns);
                getTextControl().setEchoChar('*');
            }
        });

        // Model name
        addField(new StringFieldEditor(
                VibePreferenceConstants.PREF_OPENAI_EMBEDDING_MODEL,
                Messages.EmbeddingPreferencePage_ModelLabel,
                embeddingGroup));

        // Dimensions
        IntegerFieldEditor dimensionsField = new IntegerFieldEditor(
                VibePreferenceConstants.PREF_EMBEDDING_DIMENSIONS,
                Messages.EmbeddingPreferencePage_DimensionsLabel,
                embeddingGroup);
        dimensionsField.setValidRange(64, 4096);
        addField(dimensionsField);

        // Batch size
        IntegerFieldEditor batchField = new IntegerFieldEditor(
                VibePreferenceConstants.PREF_EMBEDDING_BATCH_SIZE,
                Messages.EmbeddingPreferencePage_BatchSizeLabel,
                embeddingGroup);
        batchField.setValidRange(1, 1000);
        addField(batchField);

        // Test connection button and status
        createTestConnectionControls(embeddingGroup);
    }

    /**
     * Creates the test connection button and status label.
     */
    private void createTestConnectionControls(Composite parent) {
        // Spacer
        new Label(parent, SWT.NONE);

        // Container for button and status
        Composite buttonContainer = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = 0;
        layout.marginHeight = 5;
        buttonContainer.setLayout(layout);
        buttonContainer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        // Test connection button
        testConnectionButton = new Button(buttonContainer, SWT.PUSH);
        testConnectionButton.setText(Messages.EmbeddingPreferencePage_TestConnectionButton);
        testConnectionButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                testConnection();
            }
        });

        // Status label
        statusLabel = new Label(buttonContainer, SWT.NONE);
        statusLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    }

    /**
     * Tests the embedding provider connection.
     */
    private void testConnection() {
        // Save current values before testing
        performApply();

        // Disable button during test
        testConnectionButton.setEnabled(false);
        statusLabel.setText(Messages.EmbeddingPreferencePage_Testing);
        statusLabel.setForeground(null);

        // Get active provider
        IEmbeddingProvider provider = EmbeddingProviderRegistry.getInstance().getActiveProvider();
        if (provider == null || !provider.isConfigured()) {
            showTestResult(false, "No provider configured"); //$NON-NLS-1$
            return;
        }

        // Test with a simple embedding request
        CompletableFuture<EmbeddingResult> future = provider.embed("test"); //$NON-NLS-1$
        future.whenComplete((result, error) -> {
            Display.getDefault().asyncExec(() -> {
                if (testConnectionButton.isDisposed()) {
                    return;
                }
                if (error != null) {
                    showTestResult(false, error.getMessage());
                } else {
                    int dimensions = result.getEmbedding().length;
                    showTestResult(true, MessageFormat.format(
                            Messages.EmbeddingPreferencePage_TestSuccess, dimensions));
                }
            });
        });
    }

    /**
     * Shows the test result in the status label.
     */
    private void showTestResult(boolean success, String message) {
        testConnectionButton.setEnabled(true);
        if (success) {
            statusLabel.setText(message);
            statusLabel.setForeground(statusLabel.getDisplay().getSystemColor(SWT.COLOR_DARK_GREEN));
        } else {
            statusLabel.setText(MessageFormat.format(Messages.EmbeddingPreferencePage_TestFailed, message));
            statusLabel.setForeground(statusLabel.getDisplay().getSystemColor(SWT.COLOR_RED));
        }
        statusLabel.getParent().layout(true);
    }

    private void createIndexingGroup(Composite parent) {
        Group indexingGroup = createGroup(parent, Messages.EmbeddingPreferencePage_IndexingGroupTitle);

        // Enable indexing
        addField(new BooleanFieldEditor(
                VibePreferenceConstants.PREF_INDEXING_ENABLED,
                Messages.EmbeddingPreferencePage_EnableIndexingLabel,
                indexingGroup));

        // Index on startup
        addField(new BooleanFieldEditor(
                VibePreferenceConstants.PREF_INDEXING_ON_STARTUP,
                Messages.EmbeddingPreferencePage_IndexOnStartupLabel,
                indexingGroup));

        // Max file size
        IntegerFieldEditor maxFileSizeField = new IntegerFieldEditor(
                VibePreferenceConstants.PREF_INDEXING_MAX_FILE_SIZE,
                Messages.EmbeddingPreferencePage_MaxFileSizeLabel,
                indexingGroup);
        maxFileSizeField.setValidRange(1000, 10000000);
        addField(maxFileSizeField);
    }

    private Group createGroup(Composite parent, String title) {
        Group group = new Group(parent, SWT.NONE);
        group.setText(title);
        group.setLayout(new GridLayout(2, false));
        GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gd.horizontalSpan = 2;
        group.setLayoutData(gd);
        return group;
    }
}
