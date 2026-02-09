/*
 * Copyright (c) 2024 Example
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3.
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package com.codepilot1c.core.permissions;

import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Правило разрешений для инструмента.
 *
 * <p>Правило определяет, какое действие разрешено для инструмента
 * при работе с определенными ресурсами (файлы, команды и т.д.).</p>
 *
 * <h2>Примеры правил</h2>
 * <pre>
 * // Разрешить чтение любых файлов
 * PermissionRule.allow("read_file").forAllResources();
 *
 * // Спрашивать перед редактированием файлов
 * PermissionRule.ask("edit_file").forAllResources();
 *
 * // Запретить удаление файлов вне workspace
 * PermissionRule.deny("write_file").forResourcePattern("/etc/**");
 *
 * // Разрешить shell команды только для git
 * PermissionRule.allow("shell").forResourcePattern("git *");
 * </pre>
 */
public class PermissionRule {

    private final String toolName;
    private final PermissionDecision decision;
    private final String resourcePattern;
    private final boolean isGlob;
    private final int priority;
    private final String description;

    // Cached matchers
    private transient PathMatcher pathMatcher;
    private transient Pattern regexPattern;

    private PermissionRule(Builder builder) {
        this.toolName = Objects.requireNonNull(builder.toolName, "toolName");
        this.decision = Objects.requireNonNull(builder.decision, "decision");
        this.resourcePattern = builder.resourcePattern;
        this.isGlob = builder.isGlob;
        this.priority = builder.priority;
        this.description = builder.description;
    }

    /**
     * Имя инструмента (или "*" для всех).
     */
    public String getToolName() {
        return toolName;
    }

    /**
     * Решение для этого правила.
     */
    public PermissionDecision getDecision() {
        return decision;
    }

    /**
     * Паттерн ресурса (glob или regex).
     */
    public String getResourcePattern() {
        return resourcePattern;
    }

    /**
     * Является ли паттерн glob (иначе regex).
     */
    public boolean isGlob() {
        return isGlob;
    }

    /**
     * Приоритет правила (выше = важнее).
     */
    public int getPriority() {
        return priority;
    }

    /**
     * Описание правила для UI.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Применимо ли правило к ресурсу.
     *
     * @param resource путь к файлу или команда
     * @return true если правило применимо
     */
    public boolean matches(String resource) {
        if (resourcePattern == null || resourcePattern.equals("*")) {
            return true;
        }

        if (resource == null || resource.isEmpty()) {
            return false;
        }

        if (isGlob) {
            return matchesGlob(resource);
        } else {
            return matchesRegex(resource);
        }
    }

    /**
     * Применимо ли правило к инструменту.
     *
     * @param tool имя инструмента
     * @return true если применимо
     */
    public boolean matchesTool(String tool) {
        return toolName.equals("*") || toolName.equals(tool);
    }

    private boolean matchesGlob(String resource) {
        if (pathMatcher == null && resourcePattern != null) {
            try {
                pathMatcher = FileSystems.getDefault()
                        .getPathMatcher("glob:" + resourcePattern);
            } catch (Exception e) {
                return false;
            }
        }

        if (pathMatcher != null) {
            try {
                return pathMatcher.matches(Paths.get(resource));
            } catch (Exception e) {
                // Invalid path, try string match
                return resource.matches(globToRegex(resourcePattern));
            }
        }

        return false;
    }

    private boolean matchesRegex(String resource) {
        if (regexPattern == null && resourcePattern != null) {
            try {
                regexPattern = Pattern.compile(resourcePattern);
            } catch (Exception e) {
                return false;
            }
        }

        if (regexPattern != null) {
            return regexPattern.matcher(resource).matches();
        }

        return false;
    }

    /**
     * Простое преобразование glob в regex для строкового сравнения.
     */
    private String globToRegex(String glob) {
        StringBuilder regex = new StringBuilder("^");
        for (char c : glob.toCharArray()) {
            switch (c) {
                case '*':
                    regex.append(".*");
                    break;
                case '?':
                    regex.append(".");
                    break;
                case '.':
                case '(':
                case ')':
                case '[':
                case ']':
                case '{':
                case '}':
                case '^':
                case '$':
                case '|':
                case '\\':
                    regex.append("\\").append(c);
                    break;
                default:
                    regex.append(c);
            }
        }
        regex.append("$");
        return regex.toString();
    }

    // --- Factory methods ---

    /**
     * Создает правило ALLOW для инструмента.
     */
    public static Builder allow(String toolName) {
        return new Builder(toolName, PermissionDecision.ALLOW);
    }

    /**
     * Создает правило DENY для инструмента.
     */
    public static Builder deny(String toolName) {
        return new Builder(toolName, PermissionDecision.DENY);
    }

    /**
     * Создает правило ASK для инструмента.
     */
    public static Builder ask(String toolName) {
        return new Builder(toolName, PermissionDecision.ASK);
    }

    /**
     * Создает builder.
     */
    public static Builder builder(String toolName, PermissionDecision decision) {
        return new Builder(toolName, decision);
    }

    @Override
    public String toString() {
        return "PermissionRule{" +
                "tool='" + toolName + '\'' +
                ", decision=" + decision +
                ", pattern='" + resourcePattern + '\'' +
                ", priority=" + priority +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PermissionRule that = (PermissionRule) o;
        return Objects.equals(toolName, that.toolName) &&
                decision == that.decision &&
                Objects.equals(resourcePattern, that.resourcePattern);
    }

    @Override
    public int hashCode() {
        return Objects.hash(toolName, decision, resourcePattern);
    }

    /**
     * Builder для PermissionRule.
     */
    public static class Builder {
        private final String toolName;
        private final PermissionDecision decision;
        private String resourcePattern = "*";
        private boolean isGlob = true;
        private int priority = 0;
        private String description;

        private Builder(String toolName, PermissionDecision decision) {
            this.toolName = toolName;
            this.decision = decision;
        }

        /**
         * Применить ко всем ресурсам.
         */
        public PermissionRule forAllResources() {
            this.resourcePattern = "*";
            return build();
        }

        /**
         * Применить к ресурсам по паттерну (glob).
         */
        public Builder forResourcePattern(String pattern) {
            this.resourcePattern = pattern;
            this.isGlob = true;
            return this;
        }

        /**
         * Применить к ресурсам по regex.
         */
        public Builder forResourceRegex(String regex) {
            this.resourcePattern = regex;
            this.isGlob = false;
            return this;
        }

        /**
         * Установить приоритет (выше = важнее).
         */
        public Builder withPriority(int priority) {
            this.priority = priority;
            return this;
        }

        /**
         * Установить описание.
         */
        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }

        public PermissionRule build() {
            return new PermissionRule(this);
        }
    }
}
