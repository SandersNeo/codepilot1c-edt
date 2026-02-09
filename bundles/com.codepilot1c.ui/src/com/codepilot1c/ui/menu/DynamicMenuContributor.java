/*
 * Copyright (c) 2024 Example
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.codepilot1c.ui.menu;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.bindings.TriggerSequence;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.keys.IBindingService;
import org.eclipse.ui.texteditor.ITextEditor;

import com.codepilot1c.core.context.CodeContextAnalyzer;
import com.codepilot1c.core.context.CodeContextAnalyzer.ActionCategory;
import com.codepilot1c.core.context.CodeContextAnalyzer.AnalysisResult;
import com.codepilot1c.core.context.CodeContextAnalyzer.SuggestedAction;
import com.codepilot1c.core.context.Messages;
import com.codepilot1c.core.logging.VibeLogger;

/**
 * Dynamic context menu contributor that analyzes selected code
 * and provides relevant AI actions based on the code type.
 *
 * <p>This contributor creates a context-aware menu that shows different
 * actions depending on whether the selection is a method, query, variable, etc.</p>
 */
public class DynamicMenuContributor extends CompoundContributionItem {

    private static final VibeLogger.CategoryLogger LOG = VibeLogger.forClass(DynamicMenuContributor.class);

    private static final String MENU_ID = "com.codepilot1c.ui.dynamicMenu"; //$NON-NLS-1$

    private final CodeContextAnalyzer analyzer = new CodeContextAnalyzer();

    @Override
    protected IContributionItem[] getContributionItems() {
        LOG.debug("getContributionItems: начало создания пунктов меню"); //$NON-NLS-1$
        List<IContributionItem> items = new ArrayList<>();

        // Get current selection
        String selectedText = getSelectedText();
        LOG.debug("getContributionItems: выделенный текст=%s", //$NON-NLS-1$
                selectedText != null ? (selectedText.length() > 50 ? selectedText.substring(0, 50) + "..." : selectedText) : "null"); //$NON-NLS-1$ //$NON-NLS-2$

        // Analyze the selection
        AnalysisResult result = analyzer.analyze(selectedText, null, null);
        LOG.debug("getContributionItems: результат анализа - тип=%s, символ=%s", //$NON-NLS-1$
                result.getType(), result.getSymbolName());

        // Get suggested actions sorted by priority
        List<SuggestedAction> actions = new ArrayList<>(result.getSuggestedActions());
        actions.sort(Comparator.comparingInt(SuggestedAction::getPriority));
        LOG.debug("getContributionItems: предложено %d действий", actions.size()); //$NON-NLS-1$

        // Group actions by category
        Map<ActionCategory, List<SuggestedAction>> grouped = groupByCategory(actions);

        // Add Quick Actions section
        List<SuggestedAction> quickActions = grouped.get(ActionCategory.QUICK_ACTION);
        if (quickActions != null && !quickActions.isEmpty()) {
            items.add(createSeparator("quickActions", Messages.Menu_QuickActions)); //$NON-NLS-1$
            for (SuggestedAction action : quickActions) {
                items.add(createCommandItem(action));
            }
        }

        // Add Edit Actions section
        List<SuggestedAction> editActions = grouped.get(ActionCategory.EDIT_ACTION);
        if (editActions != null && !editActions.isEmpty()) {
            items.add(createSeparator("editActions", Messages.Menu_EditCode)); //$NON-NLS-1$
            for (SuggestedAction action : editActions) {
                items.add(createCommandItem(action));
            }
        }

        // Add Chat Actions section
        List<SuggestedAction> chatActions = grouped.get(ActionCategory.CHAT_ACTION);
        if (chatActions != null && !chatActions.isEmpty()) {
            items.add(new Separator("chatActions")); //$NON-NLS-1$
            for (SuggestedAction action : chatActions) {
                items.add(createCommandItem(action));
            }
        }

        // Add settings submenu
        items.add(new Separator("settings")); //$NON-NLS-1$
        items.add(createSettingsSubmenu());

        return items.toArray(new IContributionItem[0]);
    }

    /**
     * Groups actions by their category.
     */
    private Map<ActionCategory, List<SuggestedAction>> groupByCategory(List<SuggestedAction> actions) {
        Map<ActionCategory, List<SuggestedAction>> grouped = new java.util.LinkedHashMap<>();
        for (SuggestedAction action : actions) {
            grouped.computeIfAbsent(action.getCategory(), k -> new ArrayList<>()).add(action);
        }
        return grouped;
    }

    /**
     * Gets the currently selected text from the active editor.
     */
    private String getSelectedText() {
        LOG.debug("getSelectedText: получение выделенного текста"); //$NON-NLS-1$

        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window == null) {
            LOG.warn("getSelectedText: окно workbench = null"); //$NON-NLS-1$
            return null;
        }

        IWorkbenchPage page = window.getActivePage();
        if (page == null) {
            LOG.warn("getSelectedText: активная страница = null"); //$NON-NLS-1$
            return null;
        }

        IEditorPart editor = page.getActiveEditor();
        LOG.debug("getSelectedText: редактор=%s", editor != null ? editor.getClass().getName() : "null"); //$NON-NLS-1$ //$NON-NLS-2$

        if (editor instanceof ITextEditor textEditor) {
            ISelection selection = textEditor.getSelectionProvider().getSelection();
            LOG.debug("getSelectedText: selection=%s", selection != null ? selection.getClass().getName() : "null"); //$NON-NLS-1$ //$NON-NLS-2$

            if (selection instanceof ITextSelection textSelection) {
                String text = textSelection.getText();
                LOG.debug("getSelectedText: получен текст длиной %d символов", text != null ? text.length() : 0); //$NON-NLS-1$
                return text;
            }
        }
        LOG.warn("getSelectedText: не удалось получить текст из редактора"); //$NON-NLS-1$
        return null;
    }

    /**
     * Creates a separator with an optional label.
     */
    private IContributionItem createSeparator(String id, String label) {
        if (label == null || label.isEmpty()) {
            return new Separator(id);
        }

        return new ContributionItem(id) {
            @Override
            public void fill(Menu menu, int index) {
                MenuItem item = new MenuItem(menu, SWT.SEPARATOR, index);
                item.setEnabled(false);

                // Add label after separator
                MenuItem labelItem = new MenuItem(menu, SWT.PUSH, index + 1);
                labelItem.setText(label);
                labelItem.setEnabled(false);
            }

            @Override
            public boolean isSeparator() {
                return true;
            }
        };
    }

    /**
     * Creates a menu item for a suggested action.
     */
    private IContributionItem createCommandItem(SuggestedAction action) {
        LOG.debug("createCommandItem: создание пункта меню id='%s', label='%s', commandId='%s'", //$NON-NLS-1$
                action.getId(), action.getLabel(), action.getCommandId());

        return new ContributionItem(action.getId()) {
            @Override
            public void fill(Menu menu, int index) {
                LOG.debug("fill: заполнение пункта меню '%s' в позиции %d", action.getId(), index); //$NON-NLS-1$

                MenuItem item = new MenuItem(menu, SWT.PUSH, index);

                // Set label with keyboard shortcut if available
                String label = action.getLabel();
                String shortcut = getCommandShortcut(action.getCommandId());
                if (shortcut != null && !shortcut.isEmpty()) {
                    label = label + "\t" + shortcut; //$NON-NLS-1$
                }
                item.setText(label);

                // Execute command on selection
                item.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        LOG.info("widgetSelected: пользователь выбрал пункт меню '%s' (команда: %s)", //$NON-NLS-1$
                                action.getLabel(), action.getCommandId());
                        executeCommand(action.getCommandId());
                    }
                });
            }
        };
    }

    /**
     * Gets the keyboard shortcut for a command.
     */
    private String getCommandShortcut(String commandId) {
        try {
            IBindingService bindingService = PlatformUI.getWorkbench().getService(IBindingService.class);
            ICommandService commandService = PlatformUI.getWorkbench().getService(ICommandService.class);

            if (bindingService != null && commandService != null) {
                Command command = commandService.getCommand(commandId);
                if (command != null && command.isDefined()) {
                    ParameterizedCommand paramCmd = new ParameterizedCommand(command, null);
                    TriggerSequence binding = bindingService.getBestActiveBindingFor(paramCmd);
                    if (binding != null) {
                        return binding.format();
                    }
                }
            }
        } catch (Exception e) {
            // Ignore - shortcut not available
        }
        return null;
    }

    /**
     * Executes a command by ID.
     */
    private void executeCommand(String commandId) {
        LOG.info("executeCommand: выполнение команды '%s'", commandId); //$NON-NLS-1$
        try {
            IHandlerService handlerService = PlatformUI.getWorkbench().getService(IHandlerService.class);
            if (handlerService == null) {
                LOG.error("executeCommand: IHandlerService = null, команда не может быть выполнена"); //$NON-NLS-1$
                return;
            }

            LOG.debug("executeCommand: вызов handlerService.executeCommand('%s')", commandId); //$NON-NLS-1$
            Object result = handlerService.executeCommand(commandId, null);
            LOG.debug("executeCommand: команда '%s' выполнена, результат=%s", commandId, result); //$NON-NLS-1$
        } catch (Exception e) {
            LOG.error("executeCommand: ошибка при выполнении команды '%s': %s - %s", //$NON-NLS-1$
                    commandId, e.getClass().getSimpleName(), e.getMessage());
            com.codepilot1c.ui.internal.VibeUiPlugin.log(e);
        }
    }

    /**
     * Creates the settings submenu.
     */
    private IContributionItem createSettingsSubmenu() {
        MenuManager settingsMenu = new MenuManager(
                Messages.Menu_VibeCopilot,
                "com.codepilot1c.ui.settingsMenu"); //$NON-NLS-1$

        // Add settings items
        settingsMenu.add(new ContributionItem("openPreferences") { //$NON-NLS-1$
            @Override
            public void fill(Menu menu, int index) {
                MenuItem item = new MenuItem(menu, SWT.PUSH, index);
                item.setText(MenuMessages.Menu_Settings);
                item.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        openPreferences();
                    }
                });
            }
        });

        return settingsMenu;
    }

    /**
     * Opens the Vibe preferences page.
     */
    private void openPreferences() {
        org.eclipse.ui.dialogs.PreferencesUtil.createPreferenceDialogOn(
                null,
                "com.codepilot1c.ui.preferences.VibePreferencePage", //$NON-NLS-1$
                null,
                null).open();
    }
}
