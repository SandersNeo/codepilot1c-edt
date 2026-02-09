/*
 * Copyright (c) 2024 Example
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.codepilot1c.core.internal;

import java.util.concurrent.CompletableFuture;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.BundleContext;

import com.codepilot1c.core.http.DefaultHttpClientFactory;
import com.codepilot1c.core.http.HttpClientFactory;
import com.codepilot1c.core.logging.VibeLogger;
import com.codepilot1c.core.mcp.McpServerManager;
import com.codepilot1c.core.provider.LlmProviderRegistry;
import com.codepilot1c.core.state.VibeStateService;

/**
 * The activator class controls the plug-in life cycle.
 */
public class VibeCorePlugin extends Plugin {

    public static final String PLUGIN_ID = "com.codepilot1c.core"; //$NON-NLS-1$

    private static VibeCorePlugin plugin;
    private static ILog logger;
    private HttpClientFactory httpClientFactory;

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
        logger = Platform.getLog(getClass());

        // Initialize HTTP client factory
        httpClientFactory = new DefaultHttpClientFactory();

        // Configure VibeLogger for development (DEBUG level + file logging)
        VibeLogger vibeLogger = VibeLogger.getInstance();
        vibeLogger.setMinLevel(VibeLogger.Level.DEBUG);
        vibeLogger.setLogToFile(true);
        vibeLogger.setLogToEclipse(true);

        logInfo("1C Copilot Core plugin started"); //$NON-NLS-1$
        vibeLogger.info("Core", "VibeLogger initialized. Log file: %s", vibeLogger.getLogFilePath()); //$NON-NLS-1$ //$NON-NLS-2$

        // Initialize LLM providers and set initial state.
        // If no providers are configured, plugin still starts but shows NOT_CONFIGURED.
        try {
            LlmProviderRegistry registry = LlmProviderRegistry.getInstance();
            registry.initialize();
            var active = registry.getActiveProvider();
            if (active != null && active.isConfigured()) {
                VibeStateService.getInstance().setIdle();
            } else {
                VibeStateService.getInstance().setNotConfigured(
                        "No LLM providers configured. Configure one in Preferences."); //$NON-NLS-1$
            }
        } catch (Exception e) {
            VibeStateService.getInstance().setError(e.getMessage());
            vibeLogger.error("Core", "Failed to initialize LLM providers", e); //$NON-NLS-1$ //$NON-NLS-2$
        }

        // Start enabled MCP servers asynchronously
        CompletableFuture.runAsync(() -> {
            try {
                McpServerManager.getInstance().startEnabledServers();
            } catch (Exception e) {
                vibeLogger.error("Core", "Failed to start MCP servers", e); //$NON-NLS-1$ //$NON-NLS-2$
            }
        });
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        logInfo("1C Copilot Core plugin stopping"); //$NON-NLS-1$

        // Stop all MCP servers
        try {
            McpServerManager.getInstance().stopAllServers();
        } catch (Exception e) {
            logWarn("Error stopping MCP servers", e); //$NON-NLS-1$
        }

        // Dispose HTTP client factory
        if (httpClientFactory != null) {
            try {
                httpClientFactory.dispose();
            } catch (Exception e) {
                logWarn("Error disposing HTTP client factory", e); //$NON-NLS-1$
            }
            httpClientFactory = null;
        }

        try {
            LlmProviderRegistry.getInstance().dispose();
        } catch (Exception e) {
            logWarn("Error disposing LLM provider registry", e); //$NON-NLS-1$
        }
        plugin = null;
        super.stop(context);
    }

    /**
     * Returns the shared instance.
     *
     * @return the shared instance
     */
    public static VibeCorePlugin getDefault() {
        return plugin;
    }

    /**
     * Returns the HTTP client factory.
     *
     * @return the HTTP client factory
     */
    public HttpClientFactory getHttpClientFactory() {
        return httpClientFactory;
    }

    /**
     * Logs an info message.
     *
     * @param message the message
     */
    public static void logInfo(String message) {
        if (logger != null) {
            logger.log(new Status(IStatus.INFO, PLUGIN_ID, message));
        }
    }

    /**
     * Logs an error.
     *
     * @param message the message
     * @param e the exception
     */
    public static void logError(String message, Throwable e) {
        if (logger != null) {
            logger.log(new Status(IStatus.ERROR, PLUGIN_ID, message, e));
        }
    }

    /**
     * Logs an error.
     *
     * @param e the exception
     */
    public static void logError(Throwable e) {
        logError(e.getMessage(), e);
    }

    /**
     * Logs a warning message.
     *
     * @param message the message
     */
    public static void logWarn(String message) {
        if (logger != null) {
            logger.log(new Status(IStatus.WARNING, PLUGIN_ID, message));
        }
    }

    /**
     * Logs a warning message with exception.
     *
     * @param message the message
     * @param e the exception
     */
    public static void logWarn(String message, Throwable e) {
        if (logger != null) {
            logger.log(new Status(IStatus.WARNING, PLUGIN_ID, message, e));
        }
    }
}
