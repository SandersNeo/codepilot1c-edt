/*
 * Copyright (c) 2024 Example
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.codepilot1c.core.settings;

import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;

import com.codepilot1c.core.internal.VibeCorePlugin;

/**
 * Provides access to customizable prompt templates stored in Eclipse preferences.
 */
public final class PromptTemplateService {

    private static final ILog LOG = Platform.getLog(PromptTemplateService.class);
    private static PromptTemplateService instance;

    private PromptTemplateService() {
    }

    /**
     * Returns singleton instance.
     *
     * @return service instance
     */
    public static synchronized PromptTemplateService getInstance() {
        if (instance == null) {
            instance = new PromptTemplateService();
        }
        return instance;
    }

    /**
     * Applies user-defined system prefix/suffix around a built-in system prompt.
     *
     * @param basePrompt built-in prompt text
     * @return effective prompt
     */
    public String applySystemPrompt(String basePrompt) {
        String prefix = getPreference(VibePreferenceConstants.PREF_PROMPT_SYSTEM_PREFIX);
        String suffix = getPreference(VibePreferenceConstants.PREF_PROMPT_SYSTEM_SUFFIX);
        String base = basePrompt != null ? basePrompt : ""; //$NON-NLS-1$

        StringBuilder sb = new StringBuilder();
        if (!isBlank(prefix)) {
            sb.append(prefix.trim()).append("\n\n"); //$NON-NLS-1$
        }
        sb.append(base);
        if (!isBlank(suffix)) {
            if (sb.length() > 0 && !base.endsWith("\n")) { //$NON-NLS-1$
                sb.append("\n\n"); //$NON-NLS-1$
            }
            sb.append(suffix.trim());
        }

        return sb.toString();
    }

    /**
     * Resolves a template (custom if configured, otherwise default) and applies
     * variable replacement with placeholders in the form {@code {{name}}}.
     *
     * @param preferenceKey preference key for custom template
     * @param defaultTemplate built-in template
     * @param variables variables to replace
     * @return final prompt text
     */
    public String applyTemplate(String preferenceKey, String defaultTemplate, Map<String, String> variables) {
        return applyTemplate(preferenceKey, defaultTemplate, variables, Set.of());
    }

    /**
     * Resolves a template with placeholder validation and applies variable replacement.
     *
     * @param preferenceKey preference key for custom template
     * @param defaultTemplate built-in template
     * @param variables variables to replace
     * @param requiredPlaceholders placeholders that must be present in template
     * @return final prompt text
     */
    public String applyTemplate(
            String preferenceKey,
            String defaultTemplate,
            Map<String, String> variables,
            Set<String> requiredPlaceholders) {

        String template = resolveTemplateWithValidation(preferenceKey, defaultTemplate, requiredPlaceholders);
        if (variables == null || variables.isEmpty()) {
            return template;
        }

        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String key = entry.getKey();
            if (key == null || key.isEmpty()) {
                continue;
            }
            String value = entry.getValue() != null ? entry.getValue() : ""; //$NON-NLS-1$
            result = result.replace("{{" + key + "}}", value); //$NON-NLS-1$ //$NON-NLS-2$
        }
        return result;
    }

    private String resolveTemplateWithValidation(
            String preferenceKey,
            String defaultTemplate,
            Set<String> requiredPlaceholders) {

        String custom = getPreference(preferenceKey);
        if (isBlank(custom)) {
            return defaultTemplate != null ? defaultTemplate : ""; //$NON-NLS-1$
        }

        if (requiredPlaceholders == null || requiredPlaceholders.isEmpty()) {
            return custom;
        }

        for (String placeholder : requiredPlaceholders) {
            if (placeholder == null || placeholder.isBlank()) {
                continue;
            }
            String token = "{{" + placeholder + "}}"; //$NON-NLS-1$ //$NON-NLS-2$
            if (!custom.contains(token)) {
                logWarning("Шаблон '" + preferenceKey //$NON-NLS-1$
                        + "' не содержит обязательный плейсхолдер " + token //$NON-NLS-1$
                        + ". Используется встроенный шаблон."); //$NON-NLS-1$
                return defaultTemplate != null ? defaultTemplate : ""; //$NON-NLS-1$
            }
        }

        return custom;
    }

    /**
     * Returns custom template if defined; otherwise returns default.
     *
     * @param preferenceKey preference key
     * @param defaultTemplate built-in template
     * @return effective template
     */
    public String getTemplate(String preferenceKey, String defaultTemplate) {
        String custom = getPreference(preferenceKey);
        if (isBlank(custom)) {
            return defaultTemplate != null ? defaultTemplate : ""; //$NON-NLS-1$
        }
        return custom;
    }

    private String getPreference(String key) {
        return getPreferences().get(key, ""); //$NON-NLS-1$
    }

    private IEclipsePreferences getPreferences() {
        return InstanceScope.INSTANCE.getNode(VibeCorePlugin.PLUGIN_ID);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void logWarning(String message) {
        LOG.log(new Status(IStatus.WARNING, VibeCorePlugin.PLUGIN_ID, message));
    }
}
