/*
 * Copyright (c) 2024 Example
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.codepilot1c.core.embedding;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.BackingStoreException;

import com.codepilot1c.core.embedding.LocalModelDetector.DetectionResult;
import com.codepilot1c.core.internal.VibeCorePlugin;
import com.codepilot1c.core.settings.VibePreferenceConstants;

/**
 * Registry for embedding providers.
 *
 * <p>This class manages the lifecycle and access to registered embedding providers.</p>
 */
public final class EmbeddingProviderRegistry {

    private static final String EXTENSION_POINT_ID = VibeCorePlugin.PLUGIN_ID + ".embeddingProvider"; //$NON-NLS-1$

    private static EmbeddingProviderRegistry instance;

    private final Map<String, IEmbeddingProvider> providers = new LinkedHashMap<>();
    private boolean initialized = false;

    private EmbeddingProviderRegistry() {
        // Singleton
    }

    /**
     * Returns the singleton instance.
     *
     * @return the registry instance
     */
    public static synchronized EmbeddingProviderRegistry getInstance() {
        if (instance == null) {
            instance = new EmbeddingProviderRegistry();
        }
        return instance;
    }

    /**
     * Initializes the registry by loading providers from extension points.
     */
    public synchronized void initialize() {
        if (initialized) {
            return;
        }

        IExtensionRegistry registry = Platform.getExtensionRegistry();
        if (registry == null) {
            VibeCorePlugin.logWarn("Extension registry not available for embedding providers"); //$NON-NLS-1$
            return;
        }

        IConfigurationElement[] elements = registry.getConfigurationElementsFor(EXTENSION_POINT_ID);
        for (IConfigurationElement element : elements) {
            if ("provider".equals(element.getName())) { //$NON-NLS-1$
                try {
                    loadProvider(element);
                } catch (Exception e) {
                    VibeCorePlugin.logError("Failed to load embedding provider: " + element.getAttribute("id"), e); //$NON-NLS-1$ //$NON-NLS-2$
                }
            }
        }

        initialized = true;
        VibeCorePlugin.logInfo("Loaded " + providers.size() + " embedding providers"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private void loadProvider(IConfigurationElement element) throws CoreException {
        String id = element.getAttribute("id"); //$NON-NLS-1$
        if (id == null || id.isEmpty()) {
            VibeCorePlugin.logWarn("Embedding provider without ID found, skipping"); //$NON-NLS-1$
            return;
        }

        Object providerInstance = element.createExecutableExtension("class"); //$NON-NLS-1$
        if (providerInstance instanceof IEmbeddingProvider provider) {
            providers.put(id, provider);
            VibeCorePlugin.logInfo("Registered embedding provider: " + id); //$NON-NLS-1$
        } else {
            VibeCorePlugin.logWarn("Provider " + id + " does not implement IEmbeddingProvider"); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    /**
     * Returns all registered providers.
     *
     * @return collection of providers
     */
    public Collection<IEmbeddingProvider> getProviders() {
        initialize();
        return Collections.unmodifiableCollection(providers.values());
    }

    /**
     * Returns a provider by its ID.
     *
     * @param id the provider ID
     * @return the provider, or null if not found
     */
    public IEmbeddingProvider getProvider(String id) {
        initialize();
        return providers.get(id);
    }

    /**
     * Returns the currently active provider based on preferences.
     *
     * <p>If auto-detect mode is enabled (default), this method will automatically
     * select the best available provider without requiring configuration:</p>
     * <ol>
     *   <li>Ollama (if running locally with embedding model)</li>
     *   <li>OpenAI (if API key is configured)</li>
     * </ol>
     *
     * @return the active provider, or null if none available
     */
    public IEmbeddingProvider getActiveProvider() {
        initialize();
        IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(VibeCorePlugin.PLUGIN_ID);
        String providerId = prefs.get(VibePreferenceConstants.PREF_EMBEDDING_PROVIDER_ID,
                VibePreferenceConstants.PREF_EMBEDDING_PROVIDER_AUTO);

        // Handle auto-detection mode
        if (VibePreferenceConstants.PREF_EMBEDDING_PROVIDER_AUTO.equals(providerId)
                || prefs.getBoolean(VibePreferenceConstants.PREF_EMBEDDING_AUTO_DETECT, true)) {
            return getAutoDetectedProvider();
        }

        return getProvider(providerId);
    }

    /**
     * Auto-detects and returns the best available embedding provider.
     *
     * <p>Priority: Ollama (local) → OpenAI (cloud) → null</p>
     *
     * @return the detected provider, or null if none available
     */
    public IEmbeddingProvider getAutoDetectedProvider() {
        initialize();

        LocalModelDetector detector = new LocalModelDetector();
        DetectionResult result = detector.detect();

        if (result.isAvailable()) {
            IEmbeddingProvider provider = getProvider(result.getProviderId());
            if (provider != null && provider.isConfigured()) {
                VibeCorePlugin.logInfo("Auto-detected embedding provider: " + result.getMessage()); //$NON-NLS-1$
                return provider;
            }
        }

        VibeCorePlugin.logWarn("No embedding provider available: " + result.getMessage()); //$NON-NLS-1$
        return null;
    }

    /**
     * Detects available providers asynchronously.
     *
     * @return a future containing the detection result
     */
    public CompletableFuture<DetectionResult> detectProvidersAsync() {
        return new LocalModelDetector().detectAsync();
    }

    /**
     * Returns the first configured provider using the fallback chain.
     *
     * <p>Fallback order: specified provider → Ollama → OpenAI → null</p>
     *
     * @param preferredId the preferred provider ID (can be null)
     * @return a configured provider, or null if none available
     */
    public IEmbeddingProvider getProviderWithFallback(String preferredId) {
        initialize();

        // Try preferred provider first
        if (preferredId != null && !VibePreferenceConstants.PREF_EMBEDDING_PROVIDER_AUTO.equals(preferredId)) {
            IEmbeddingProvider preferred = getProvider(preferredId);
            if (preferred != null && preferred.isConfigured()) {
                return preferred;
            }
        }

        // Fallback chain: Ollama → OpenAI
        String[] fallbackOrder = {"ollama", "openai"}; //$NON-NLS-1$ //$NON-NLS-2$
        for (String id : fallbackOrder) {
            IEmbeddingProvider provider = getProvider(id);
            if (provider != null && provider.isConfigured()) {
                return provider;
            }
        }

        return null;
    }

    /**
     * Sets the active provider.
     *
     * @param id the provider ID
     */
    public void setActiveProvider(String id) {
        initialize();
        if (!providers.containsKey(id)) {
            throw new IllegalArgumentException("Unknown embedding provider: " + id); //$NON-NLS-1$
        }
        IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(VibeCorePlugin.PLUGIN_ID);
        prefs.put(VibePreferenceConstants.PREF_EMBEDDING_PROVIDER_ID, id);
        try {
            prefs.flush();
        } catch (BackingStoreException e) {
            VibeCorePlugin.logWarn("Failed to persist embedding provider preference", e); //$NON-NLS-1$
        }
    }

    /**
     * Disposes all providers and clears the registry.
     */
    public synchronized void dispose() {
        for (IEmbeddingProvider provider : providers.values()) {
            try {
                provider.dispose();
            } catch (Exception e) {
                VibeCorePlugin.logWarn("Error disposing embedding provider: " + provider.getId(), e); //$NON-NLS-1$
            }
        }
        providers.clear();
        initialized = false;
    }
}
