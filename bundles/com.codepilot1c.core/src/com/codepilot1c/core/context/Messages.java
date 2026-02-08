/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
 */
package com.codepilot1c.core.context;

import org.eclipse.osgi.util.NLS;

/**
 * Localized messages for code context analysis.
 */
public class Messages extends NLS {

    private static final String BUNDLE_NAME = "com.codepilot1c.core.context.messages"; //$NON-NLS-1$

    // Quick Actions
    public static String Action_ExplainCode;
    public static String Action_ExplainMethod;
    public static String Action_ExplainQuery;
    public static String Action_ExplainVariable;
    public static String Action_FindSimilar;
    public static String Action_OptimizeQuery;

    // Edit Actions
    public static String Action_Review;
    public static String Action_Fix;
    public static String Action_GenerateDoc;
    public static String Action_AddCode;
    public static String Action_GenerateCode;

    // Chat Actions
    public static String Action_OpenChat;

    // Menu labels
    public static String Menu_QuickActions;
    public static String Menu_EditCode;
    public static String Menu_VibeCopilot;

    static {
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
