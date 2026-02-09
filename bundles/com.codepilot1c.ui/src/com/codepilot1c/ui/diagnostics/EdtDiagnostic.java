/*
 * Copyright (c) 2024 Example
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.codepilot1c.ui.diagnostics;

/**
 * Represents a single EDT diagnostic (error, warning, info).
 */
public record EdtDiagnostic(
        String filePath,
        int lineNumber,
        int charStart,
        int charEnd,
        String message,
        Severity severity,
        String markerType,
        String source,
        String codeSnippet) {

    /**
     * Diagnostic severity level.
     */
    public enum Severity {
        ERROR(2),
        WARNING(1),
        INFO(0);

        private final int level;

        Severity(int level) {
            this.level = level;
        }

        public int getLevel() {
            return level;
        }

        /**
         * Converts Eclipse IMarker severity to enum.
         */
        public static Severity fromMarkerSeverity(int severity) {
            return switch (severity) {
                case 2 -> ERROR;   // IMarker.SEVERITY_ERROR
                case 1 -> WARNING; // IMarker.SEVERITY_WARNING
                default -> INFO;   // IMarker.SEVERITY_INFO
            };
        }
    }

    /**
     * Creates a diagnostic from marker data.
     */
    public static EdtDiagnostic fromMarker(
            String filePath,
            int lineNumber,
            int charStart,
            int charEnd,
            String message,
            int markerSeverity,
            String markerType,
            String codeSnippet) {
        return new EdtDiagnostic(
                filePath,
                lineNumber,
                charStart,
                charEnd,
                message,
                Severity.fromMarkerSeverity(markerSeverity),
                markerType,
                "marker", //$NON-NLS-1$
                codeSnippet);
    }

    /**
     * Creates a diagnostic from annotation data.
     */
    public static EdtDiagnostic fromAnnotation(
            String filePath,
            int lineNumber,
            int charStart,
            int charEnd,
            String message,
            Severity severity,
            String annotationType,
            String codeSnippet) {
        return new EdtDiagnostic(
                filePath,
                lineNumber,
                charStart,
                charEnd,
                message,
                severity,
                annotationType,
                "annotation", //$NON-NLS-1$
                codeSnippet);
    }

    /**
     * Formats diagnostic for LLM output.
     */
    public String formatForLlm() {
        StringBuilder sb = new StringBuilder();
        sb.append("- **").append(severity.name()).append("** строка ").append(lineNumber); //$NON-NLS-1$ //$NON-NLS-2$
        if (charStart >= 0 && charEnd >= 0) {
            sb.append(" (позиция ").append(charStart).append("-").append(charEnd).append(")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
        sb.append(": ").append(message); //$NON-NLS-1$
        if (codeSnippet != null && !codeSnippet.isBlank()) {
            sb.append("\n  ```\n  ").append(codeSnippet.trim()).append("\n  ```"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        return sb.toString();
    }
}
