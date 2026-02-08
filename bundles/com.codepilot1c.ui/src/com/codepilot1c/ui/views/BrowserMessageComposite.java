/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
 */
package com.codepilot1c.ui.views;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import com.codepilot1c.ui.internal.VibeUiPlugin;
import com.codepilot1c.ui.markdown.SimpleMarkdownParser;
import com.codepilot1c.ui.theme.ThemeManager;

/**
 * Composite для отображения сообщения чата с использованием SWT Browser.
 *
 * <p>Использует HTML/CSS для рендеринга Markdown с поддержкой:</p>
 * <ul>
 *   <li>Таблицы с правильным форматированием</li>
 *   <li>Блоки кода с подсветкой синтаксиса</li>
 *   <li>Кнопки копирования кода</li>
 *   <li>Темная/светлая тема</li>
 * </ul>
 */
public class BrowserMessageComposite extends Composite {

    private static final String CSS_RESOURCE = "/resources/chat.css"; //$NON-NLS-1$
    private static final String JS_RESOURCE = "/resources/chat.js"; //$NON-NLS-1$

    private static String cachedCss;
    private static String cachedJs;

    private final Browser browser;
    private final SimpleMarkdownParser markdownParser;
    private final String sender;
    private final String message;
    private final boolean isAssistant;

    private BrowserFunction copyFunction;
    private BrowserFunction openUrlFunction;

    /**
     * Создает новый composite для отображения сообщения.
     *
     * @param parent родительский composite
     * @param sender имя отправителя
     * @param message содержимое сообщения (Markdown)
     * @param isAssistant true если это сообщение от AI
     */
    public BrowserMessageComposite(Composite parent, String sender, String message, boolean isAssistant) {
        super(parent, SWT.NONE);
        this.sender = sender;
        this.message = message != null ? message : ""; //$NON-NLS-1$
        this.isAssistant = isAssistant;
        this.markdownParser = new SimpleMarkdownParser();

        setLayout(new FillLayout());

        // Try to create browser, fallback to null if not available
        Browser tempBrowser = null;
        try {
            // Try Edge first on Windows
            tempBrowser = new Browser(this, SWT.EDGE);
        } catch (SWTError e1) {
            try {
                // Fallback to default browser
                tempBrowser = new Browser(this, SWT.NONE);
            } catch (SWTError e2) {
                // Browser not available
                VibeUiPlugin.log(e2); //$NON-NLS-1$
            }
        }
        this.browser = tempBrowser;

        if (browser != null) {
            setupBrowser();
            renderMessage();
        }
    }

    /**
     * Настраивает браузер и регистрирует JavaScript-функции.
     */
    private void setupBrowser() {
        // Disable context menu
        browser.addMenuDetectListener(e -> e.doit = false);

        // Register copy-to-clipboard function
        copyFunction = new BrowserFunction(browser, "copyToClipboard") { //$NON-NLS-1$
            @Override
            public Object function(Object[] arguments) {
                if (arguments.length > 0 && arguments[0] instanceof String) {
                    String text = (String) arguments[0];
                    copyToClipboard(text);
                }
                return null;
            }
        };

        // Register open URL function
        openUrlFunction = new BrowserFunction(browser, "openUrl") { //$NON-NLS-1$
            @Override
            public Object function(Object[] arguments) {
                if (arguments.length > 0 && arguments[0] instanceof String) {
                    String url = (String) arguments[0];
                    openExternalUrl(url);
                }
                return null;
            }
        };
    }

    /**
     * Рендерит сообщение в браузере.
     */
    private void renderMessage() {
        String html = buildHtmlDocument();
        browser.setText(html);
    }

    /**
     * Создает полный HTML-документ для отображения сообщения.
     */
    private String buildHtmlDocument() {
        String css = loadCss();
        String js = loadJs();
        String themeClass = ThemeManager.getInstance().isDarkTheme() ? "dark" : "light"; //$NON-NLS-1$ //$NON-NLS-2$
        String messageClass = isAssistant ? "assistant" : "user"; //$NON-NLS-1$ //$NON-NLS-2$
        String messageHtml = markdownParser.toHtml(message);

        return "<!DOCTYPE html>\n" + //$NON-NLS-1$
               "<html>\n" + //$NON-NLS-1$
               "<head>\n" + //$NON-NLS-1$
               "    <meta charset=\"UTF-8\">\n" + //$NON-NLS-1$
               "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" + //$NON-NLS-1$
               "    <style>\n" + css + "\n    </style>\n" + //$NON-NLS-1$ //$NON-NLS-2$
               "</head>\n" + //$NON-NLS-1$
               "<body class=\"" + themeClass + "\">\n" + //$NON-NLS-1$ //$NON-NLS-2$
               "    <div class=\"message-container\">\n" + //$NON-NLS-1$
               "        <div class=\"message " + messageClass + "\">\n" + //$NON-NLS-1$ //$NON-NLS-2$
               "            <div class=\"message-header\">\n" + //$NON-NLS-1$
               "                <span class=\"message-sender\">" + escapeHtml(sender) + "</span>\n" + //$NON-NLS-1$ //$NON-NLS-2$
               "            </div>\n" + //$NON-NLS-1$
               "            <div class=\"message-content\">\n" + //$NON-NLS-1$
               messageHtml + "\n" + //$NON-NLS-1$
               "            </div>\n" + //$NON-NLS-1$
               "        </div>\n" + //$NON-NLS-1$
               "    </div>\n" + //$NON-NLS-1$
               "    <script>\n" + js + "\n    </script>\n" + //$NON-NLS-1$ //$NON-NLS-2$
               "</body>\n" + //$NON-NLS-1$
               "</html>"; //$NON-NLS-1$
    }

    /**
     * Загружает CSS из ресурсов.
     */
    private String loadCss() {
        if (cachedCss != null) {
            return cachedCss;
        }

        cachedCss = loadResource(CSS_RESOURCE);
        if (cachedCss == null) {
            cachedCss = getDefaultCss();
        }
        return cachedCss;
    }

    /**
     * Загружает JavaScript из ресурсов.
     */
    private String loadJs() {
        if (cachedJs != null) {
            return cachedJs;
        }

        cachedJs = loadResource(JS_RESOURCE);
        if (cachedJs == null) {
            cachedJs = getDefaultJs();
        }
        return cachedJs;
    }

    /**
     * Загружает ресурс из bundle.
     */
    private String loadResource(String path) {
        try {
            URL url = VibeUiPlugin.getDefault().getBundle().getEntry(path);
            if (url != null) {
                try (InputStream is = url.openStream();
                     BufferedReader reader = new BufferedReader(
                             new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    return reader.lines().collect(Collectors.joining("\n")); //$NON-NLS-1$
                }
            }
        } catch (IOException e) {
            VibeUiPlugin.log(e);
        }
        return null;
    }

    /**
     * Возвращает CSS по умолчанию (fallback).
     */
    private String getDefaultCss() {
        return """
            body {
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                font-size: 14px;
                line-height: 1.6;
                margin: 0;
                padding: 12px;
                background: #ffffff;
                color: #1a1a2e;
            }
            body.dark {
                background: #1e293b;
                color: #f1f5f9;
            }
            table {
                border-collapse: collapse;
                width: 100%;
                margin: 12px 0;
            }
            th, td {
                border: 1px solid #e2e8f0;
                padding: 8px 12px;
                text-align: left;
            }
            body.dark th, body.dark td {
                border-color: #334155;
            }
            pre {
                background: #f1f5f9;
                padding: 12px;
                border-radius: 6px;
                overflow-x: auto;
            }
            body.dark pre {
                background: #0f172a;
            }
            code {
                font-family: 'Consolas', 'Monaco', monospace;
                font-size: 13px;
            }
            """;
    }

    /**
     * Возвращает JavaScript по умолчанию (fallback).
     */
    private String getDefaultJs() {
        return """
            function copyCode(button) {
                var codeBlock = button.closest('.code-block');
                var code = codeBlock.querySelector('pre code');
                var text = code.textContent || code.innerText;
                if (typeof copyToClipboard === 'function') {
                    copyToClipboard(text);
                }
                button.textContent = 'Скопировано!';
                setTimeout(function() { button.textContent = 'Копировать'; }, 2000);
            }
            """;
    }

    /**
     * Экранирует HTML-спецсимволы.
     */
    private String escapeHtml(String text) {
        if (text == null) return ""; //$NON-NLS-1$
        return text
                .replace("&", "&amp;") //$NON-NLS-1$ //$NON-NLS-2$
                .replace("<", "&lt;") //$NON-NLS-1$ //$NON-NLS-2$
                .replace(">", "&gt;") //$NON-NLS-1$ //$NON-NLS-2$
                .replace("\"", "&quot;"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * Копирует текст в буфер обмена.
     */
    private void copyToClipboard(String text) {
        Display.getDefault().asyncExec(() -> {
            Clipboard clipboard = new Clipboard(Display.getCurrent());
            try {
                clipboard.setContents(
                        new Object[] { text },
                        new Transfer[] { TextTransfer.getInstance() }
                );
            } finally {
                clipboard.dispose();
            }
        });
    }

    /**
     * Открывает URL во внешнем браузере.
     */
    private void openExternalUrl(String url) {
        Display.getDefault().asyncExec(() -> {
            Program.launch(url);
        });
    }

    /**
     * Обновляет тему отображения.
     *
     * @param isDark true для темной темы
     */
    public void updateTheme(boolean isDark) {
        if (browser != null && !browser.isDisposed()) {
            String themeClass = isDark ? "dark" : "light"; //$NON-NLS-1$ //$NON-NLS-2$
            browser.execute("document.body.className = '" + themeClass + "';"); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    /**
     * Проверяет, доступен ли браузер.
     *
     * @return true если браузер инициализирован
     */
    public boolean isBrowserAvailable() {
        return browser != null && !browser.isDisposed();
    }

    /**
     * Возвращает Browser виджет.
     *
     * @return Browser или null если недоступен
     */
    public Browser getBrowser() {
        return browser;
    }

    @Override
    public void dispose() {
        if (copyFunction != null) {
            copyFunction.dispose();
        }
        if (openUrlFunction != null) {
            openUrlFunction.dispose();
        }
        super.dispose();
    }
}
