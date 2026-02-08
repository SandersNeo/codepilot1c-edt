/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
 */
package com.codepilot1c.core.agent.prompts;

/**
 * Provides optional overrides for system prompt additions per agent profile.
 *
 * <p>Overlay bundles (e.g. Pro) can contribute an implementation via the
 * {@code com.codepilot1c.core.promptProvider} extension point.</p>
 */
public interface IPromptProvider {

    /**
     * Returns additional system prompt text for the given profile.
     *
     * @param profileId profile id (e.g. "plan", "explore", "build")
     * @return prompt text, or null/empty to indicate no override
     */
    String getSystemPromptAddition(String profileId);
}

