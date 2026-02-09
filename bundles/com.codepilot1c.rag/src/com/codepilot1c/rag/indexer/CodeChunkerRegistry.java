/*
 * Copyright (c) 2024 Example
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.codepilot1c.rag.indexer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

import com.codepilot1c.rag.internal.VibeRagPlugin;

/**
 * Registry for code chunkers contributed via extension point.
 */
public final class CodeChunkerRegistry {

    private static final String ATTR_ID = "id"; //$NON-NLS-1$
    private static final String ATTR_NAME = "name"; //$NON-NLS-1$
    private static final String ATTR_LANGUAGE = "language"; //$NON-NLS-1$
    private static final String ATTR_CLASS = "class"; //$NON-NLS-1$
    private static final String ATTR_PRIORITY = "priority"; //$NON-NLS-1$
    private static final String ELEMENT_CHUNKER = "chunker"; //$NON-NLS-1$

    private static CodeChunkerRegistry instance;

    private final Map<String, List<ChunkerDescriptor>> chunkersByLanguage = new HashMap<>();
    private final Map<String, ICodeChunker> instantiatedChunkers = new HashMap<>();
    private boolean initialized = false;

    private CodeChunkerRegistry() {
        // Private constructor
    }

    /**
     * Returns the singleton instance.
     *
     * @return the registry instance
     */
    public static synchronized CodeChunkerRegistry getInstance() {
        if (instance == null) {
            instance = new CodeChunkerRegistry();
        }
        return instance;
    }

    /**
     * Initializes the registry by reading extension point contributions.
     */
    public synchronized void initialize() {
        if (initialized) {
            return;
        }

        IExtensionRegistry registry = Platform.getExtensionRegistry();
        if (registry == null) {
            return;
        }

        IConfigurationElement[] elements = registry.getConfigurationElementsFor(
                VibeRagPlugin.CODE_CHUNKER_EXTENSION_POINT);

        for (IConfigurationElement element : elements) {
            if (ELEMENT_CHUNKER.equals(element.getName())) {
                try {
                    ChunkerDescriptor descriptor = parseDescriptor(element);
                    chunkersByLanguage
                            .computeIfAbsent(descriptor.language, k -> new ArrayList<>())
                            .add(descriptor);
                } catch (Exception e) {
                    VibeRagPlugin.logError("Failed to parse code chunker extension", e); //$NON-NLS-1$
                }
            }
        }

        // Sort by priority (descending)
        for (List<ChunkerDescriptor> descriptors : chunkersByLanguage.values()) {
            descriptors.sort(Comparator.comparingInt(d -> -d.priority));
        }

        initialized = true;
    }

    private ChunkerDescriptor parseDescriptor(IConfigurationElement element) {
        String id = element.getAttribute(ATTR_ID);
        String name = element.getAttribute(ATTR_NAME);
        String language = element.getAttribute(ATTR_LANGUAGE);
        int priority = 0;
        String priorityStr = element.getAttribute(ATTR_PRIORITY);
        if (priorityStr != null && !priorityStr.isEmpty()) {
            try {
                priority = Integer.parseInt(priorityStr);
            } catch (NumberFormatException e) {
                // Use default
            }
        }
        return new ChunkerDescriptor(id, name, language, priority, element);
    }

    /**
     * Returns a code chunker for the given file.
     *
     * @param file the file
     * @return optional containing the chunker, or empty if none found
     */
    public Optional<ICodeChunker> getChunkerForFile(IFile file) {
        initialize();

        for (List<ChunkerDescriptor> descriptors : chunkersByLanguage.values()) {
            for (ChunkerDescriptor descriptor : descriptors) {
                ICodeChunker chunker = getOrCreateChunker(descriptor);
                if (chunker != null && chunker.canHandle(file)) {
                    return Optional.of(chunker);
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Returns a code chunker for the given language.
     *
     * @param language the language identifier
     * @return optional containing the chunker, or empty if none found
     */
    public Optional<ICodeChunker> getChunkerForLanguage(String language) {
        initialize();

        List<ChunkerDescriptor> descriptors = chunkersByLanguage.get(language);
        if (descriptors == null || descriptors.isEmpty()) {
            return Optional.empty();
        }

        ICodeChunker chunker = getOrCreateChunker(descriptors.get(0));
        return Optional.ofNullable(chunker);
    }

    /**
     * Returns all registered chunkers.
     *
     * @return list of all chunkers
     */
    public List<ICodeChunker> getAllChunkers() {
        initialize();

        List<ICodeChunker> result = new ArrayList<>();
        for (List<ChunkerDescriptor> descriptors : chunkersByLanguage.values()) {
            for (ChunkerDescriptor descriptor : descriptors) {
                ICodeChunker chunker = getOrCreateChunker(descriptor);
                if (chunker != null) {
                    result.add(chunker);
                }
            }
        }
        return result;
    }

    private synchronized ICodeChunker getOrCreateChunker(ChunkerDescriptor descriptor) {
        ICodeChunker chunker = instantiatedChunkers.get(descriptor.id);
        if (chunker == null) {
            try {
                Object obj = descriptor.element.createExecutableExtension(ATTR_CLASS);
                if (obj instanceof ICodeChunker) {
                    chunker = (ICodeChunker) obj;
                    instantiatedChunkers.put(descriptor.id, chunker);
                }
            } catch (CoreException e) {
                VibeRagPlugin.logError("Failed to instantiate code chunker: " + descriptor.id, e); //$NON-NLS-1$
            }
        }
        return chunker;
    }

    /**
     * Clears the registry (for testing).
     */
    public synchronized void clear() {
        chunkersByLanguage.clear();
        instantiatedChunkers.clear();
        initialized = false;
    }

    /**
     * Descriptor for a registered code chunker.
     */
    private static class ChunkerDescriptor {
        final String id;
        final String name;
        final String language;
        final int priority;
        final IConfigurationElement element;

        ChunkerDescriptor(String id, String name, String language, int priority,
                          IConfigurationElement element) {
            this.id = id;
            this.name = name;
            this.language = language;
            this.priority = priority;
            this.element = element;
        }
    }
}
