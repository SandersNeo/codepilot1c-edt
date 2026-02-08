/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
 */
package com.codepilot1c.ui.menu;

import org.eclipse.osgi.util.NLS;

/**
 * Localized messages for dynamic menu.
 */
public class MenuMessages extends NLS {

    private static final String BUNDLE_NAME = "com.codepilot1c.ui.menu.messages"; //$NON-NLS-1$

    public static String Menu_Settings;
    public static String Menu_SelectProvider;
    public static String Menu_IndexCodebase;
    public static String Menu_ViewChat;

    static {
        NLS.initializeMessages(BUNDLE_NAME, MenuMessages.class);
    }

    private MenuMessages() {
    }
}
