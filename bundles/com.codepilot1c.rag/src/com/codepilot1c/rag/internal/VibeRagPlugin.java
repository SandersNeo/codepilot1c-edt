/*
 * Copyright (c) 2024 Example
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.codepilot1c.rag.internal;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle for the RAG bundle.
 */
public class VibeRagPlugin extends Plugin {

    /** The plug-in ID */
    public static final String PLUGIN_ID = "com.codepilot1c.rag"; //$NON-NLS-1$

    /** Extension point for code chunkers */
    public static final String CODE_CHUNKER_EXTENSION_POINT = PLUGIN_ID + ".codeChunker"; //$NON-NLS-1$

    /** The shared instance */
    private static VibeRagPlugin plugin;

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }

    /**
     * Returns the shared instance.
     *
     * @return the shared instance
     */
    public static VibeRagPlugin getDefault() {
        return plugin;
    }

    /**
     * Logs an error message.
     *
     * @param message the message
     */
    public static void logError(String message) {
        logError(message, null);
    }

    /**
     * Logs an error with exception.
     *
     * @param message the message
     * @param exception the exception
     */
    public static void logError(String message, Throwable exception) {
        if (plugin != null) {
            plugin.getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, message, exception));
        }
    }

    /**
     * Logs a warning message.
     *
     * @param message the message
     */
    public static void logWarning(String message) {
        if (plugin != null) {
            plugin.getLog().log(new Status(IStatus.WARNING, PLUGIN_ID, message));
        }
    }

    /**
     * Logs an info message.
     *
     * @param message the message
     */
    public static void logInfo(String message) {
        if (plugin != null) {
            plugin.getLog().log(new Status(IStatus.INFO, PLUGIN_ID, message));
        }
    }
}
