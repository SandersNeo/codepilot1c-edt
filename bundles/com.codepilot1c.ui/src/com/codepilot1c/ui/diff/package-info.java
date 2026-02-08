/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
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
