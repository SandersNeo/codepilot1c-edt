/*
 * Copyright (c) 2024 Example
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 * SPDX-License-Identifier: AGPL-3.0-only
 */
/**
 * Diff view and proposed change management.
 *
 * <p>Provides functionality for reviewing AI-proposed changes before applying:</p>
 * <ul>
 *   <li>{@link com.codepilot1c.ui.diff.ProposedChange} - Single proposed file change</li>
 *   <li>{@link com.codepilot1c.ui.diff.ProposedChangeSet} - Collection of proposed changes</li>
 *   <li>{@link com.codepilot1c.ui.diff.DiffReviewDialog} - UI for reviewing and accepting/rejecting changes</li>
 *   <li>{@link com.codepilot1c.ui.diff.ChangeApplicator} - Applies accepted changes to workspace</li>
 * </ul>
 */
package com.codepilot1c.ui.diff;
