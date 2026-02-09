/*
 * Copyright (c) 2024 Example
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 * SPDX-License-Identifier: AGPL-3.0-only
 */
/**
 * File editing infrastructure with fuzzy matching.
 *
 * <p>Provides Aider-style SEARCH/REPLACE block parsing and
 * multi-strategy fuzzy matching for reliable code editing.</p>
 *
 * <h2>Key Classes</h2>
 * <ul>
 *   <li>{@link com.codepilot1c.core.edit.FuzzyMatcher} - Multi-strategy text matcher</li>
 *   <li>{@link com.codepilot1c.core.edit.SearchReplaceFormat} - SEARCH/REPLACE block parser</li>
 *   <li>{@link com.codepilot1c.core.edit.FileEditApplier} - Edit application engine</li>
 *   <li>{@link com.codepilot1c.core.edit.EditBlock} - Single edit operation</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>
 * // Parse and apply SEARCH/REPLACE blocks
 * FileEditApplier applier = new FileEditApplier();
 * ApplyResult result = applier.applyFromResponse(fileContent, llmResponse);
 * if (result.allSuccessful()) {
 *     String newContent = result.afterContent();
 * } else {
 *     String feedback = result.getFailureFeedback();
 * }
 * </pre>
 *
 * @since 1.3.0
 */
package com.codepilot1c.core.edit;
