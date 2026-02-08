/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
 */
package com.codepilot1c.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.ITextEditor;

import com.codepilot1c.core.logging.VibeLogger;
import com.codepilot1c.ui.views.ChatView;

/**
 * Handler for explaining selected code using AI.
 */
public class ExplainCodeHandler extends AbstractHandler {

    private static final VibeLogger.CategoryLogger LOG = VibeLogger.forClass(ExplainCodeHandler.class);
    private static final int MAX_CONTEXT_LINES = 50;
    private static final int MAX_DOCUMENT_CHARS = 8000;

    /**
     * Контекст редактора для передачи в LLM.
     */
    protected static class EditorContext {
        String selectedText;
        String fullDocument;
        String surroundingCode;
        String filePath;
        String fileName;
        String moduleType;
        int selectionStartLine;
        int selectionEndLine;
        int totalLines;
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        LOG.info("execute: обработчик ExplainCode вызван"); //$NON-NLS-1$

        // Get window - try from event first, then fall back to PlatformUI
        IWorkbenchWindow window = null;
        if (event != null) {
            window = HandlerUtil.getActiveWorkbenchWindow(event);
            LOG.debug("execute: окно из события=%s", window != null ? "получено" : "null"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
        if (window == null) {
            window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
            LOG.debug("execute: окно из PlatformUI=%s", window != null ? "получено" : "null"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
        if (window == null) {
            LOG.error("execute: не удалось получить окно workbench, выход"); //$NON-NLS-1$
            return null;
        }

        IWorkbenchPage page = window.getActivePage();
        if (page == null) {
            LOG.error("execute: активная страница = null, выход"); //$NON-NLS-1$
            return null;
        }
        LOG.debug("execute: активная страница получена"); //$NON-NLS-1$

        // Получаем полный контекст редактора
        EditorContext context = getEditorContext(page);
        if (context == null || context.selectedText == null || context.selectedText.trim().isEmpty()) {
            LOG.warn("execute: нет кода для объяснения (выделение/курсор), выход"); //$NON-NLS-1$
            return null;
        }
        LOG.info("execute: контекст получен - файл: %s, выделено: %d симв., документ: %d симв.", //$NON-NLS-1$
                context.fileName, context.selectedText.length(),
                context.fullDocument != null ? context.fullDocument.length() : 0);

        try {
            // Open chat view and send explain request
            LOG.debug("execute: открытие ChatView..."); //$NON-NLS-1$
            ChatView chatView = (ChatView) page.showView(ChatView.ID);
            if (chatView == null) {
                LOG.error("execute: не удалось открыть ChatView (null)"); //$NON-NLS-1$
                return null;
            }
            LOG.debug("execute: ChatView открыт успешно"); //$NON-NLS-1$

            String message = buildExplainPromptWithContext(context);
            LOG.info("execute: отправка сообщения в ChatView (длина=%d)", message.length()); //$NON-NLS-1$
            chatView.sendProgrammaticMessage(message);
            LOG.debug("execute: сообщение отправлено"); //$NON-NLS-1$
        } catch (PartInitException e) {
            LOG.error("execute: ошибка при открытии ChatView: %s", e.getMessage()); //$NON-NLS-1$
            throw new ExecutionException("Failed to open Chat view", e); //$NON-NLS-1$
        }

        return null;
    }

    /**
     * Получает полный контекст из редактора.
     */
    protected EditorContext getEditorContext(IWorkbenchPage page) {
        IEditorPart editor = page.getActiveEditor();
        if (editor == null) {
            return null;
        }

        EditorContext context = new EditorContext();

        // Получаем информацию о файле
        IEditorInput input = editor.getEditorInput();
        if (input instanceof IFileEditorInput fileInput) {
            context.filePath = fileInput.getFile().getFullPath().toString();
            context.fileName = fileInput.getFile().getName();
            context.moduleType = detectModuleType(context.filePath);
        } else if (input != null) {
            context.fileName = input.getName();
        }

        // Пробуем получить ITextEditor через адаптер (работает для EDT)
        ITextEditor textEditor = null;
        if (editor instanceof ITextEditor) {
            textEditor = (ITextEditor) editor;
        } else if (editor instanceof IAdaptable) {
            textEditor = ((IAdaptable) editor).getAdapter(ITextEditor.class);
        }

        if (textEditor != null) {
            LOG.info("getEditorContext: получен ITextEditor через адаптер"); //$NON-NLS-1$
            // Используем input от адаптированного редактора
            IEditorInput textEditorInput = textEditor.getEditorInput();
            IDocument document = textEditor.getDocumentProvider().getDocument(textEditorInput);
            LOG.info("getEditorContext: document=%s", document != null ? "получен" : "null"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            if (document != null) {
                // Полный документ
                context.fullDocument = document.get();
                try {
                    context.totalLines = document.getNumberOfLines();
                } catch (Exception e) {
                    context.totalLines = 0;
                }

                // Выделенный текст и его позиция
                ISelection selection = textEditor.getSelectionProvider().getSelection();
                if (selection instanceof ITextSelection textSelection) {
                    context.selectedText = textSelection.getText();
                    int offset = textSelection.getOffset();
                    int length = textSelection.getLength();

                    try {
                        context.selectionStartLine = document.getLineOfOffset(offset);
                        context.selectionEndLine = document.getLineOfOffset(offset + Math.max(0, length));

                        // If nothing selected, fall back to current line at caret.
                        if (length <= 0) {
                            int lineOffset = document.getLineOffset(context.selectionStartLine);
                            int lineLen = document.getLineLength(context.selectionStartLine);
                            context.selectedText = document.get(lineOffset, lineLen).trim();
                        }

                        // Surrounding context
                        context.surroundingCode = getSurroundingCode(document,
                                context.selectionStartLine, context.selectionEndLine);
                    } catch (BadLocationException e) {
                        LOG.warn("getEditorContext: ошибка получения позиции: %s", e.getMessage()); //$NON-NLS-1$
                    }
                }
            }
        } else {
            // Fallback через ISourceViewer
            LOG.info("getEditorContext: ITextEditor недоступен, пробуем ISourceViewer"); //$NON-NLS-1$
            ISourceViewer sourceViewer = getSourceViewer(editor);
            if (sourceViewer != null) {
                IDocument document = sourceViewer.getDocument();
                if (document != null) {
                    context.fullDocument = document.get();
                    try {
                        context.totalLines = document.getNumberOfLines();
                    } catch (Exception e) {
                        context.totalLines = 0;
                    }

                    Point selection = sourceViewer.getSelectedRange();
                    try {
                        context.selectionStartLine = document.getLineOfOffset(selection.x);
                        context.selectionEndLine = document.getLineOfOffset(selection.x + Math.max(0, selection.y));
                        if (selection.y > 0) {
                            context.selectedText = document.get(selection.x, selection.y);
                        } else {
                            int lineOffset = document.getLineOffset(context.selectionStartLine);
                            int lineLen = document.getLineLength(context.selectionStartLine);
                            context.selectedText = document.get(lineOffset, lineLen).trim();
                        }
                        context.surroundingCode = getSurroundingCode(document,
                                context.selectionStartLine, context.selectionEndLine);
                    } catch (BadLocationException e) {
                        LOG.warn("getEditorContext: ошибка: %s", e.getMessage()); //$NON-NLS-1$
                    }
                }

                // Fallback через StyledText
                if (context.selectedText == null || context.selectedText.isEmpty()) {
                    StyledText textWidget = sourceViewer.getTextWidget();
                    if (textWidget != null && !textWidget.isDisposed()) {
                        context.selectedText = textWidget.getSelectionText();
                    }
                }
            }
        }

        // Последний fallback
        if (context.selectedText == null || context.selectedText.isEmpty()) {
            context.selectedText = getSelectedText(page);
        }

        return context;
    }

    /**
     * Проверяет, является ли файл BSL модулем.
     */
    private boolean isBslFile(String filePath, String fileName) {
        // Проверяем по пути (содержит расширение)
        if (filePath != null) {
            String lower = filePath.toLowerCase();
            if (lower.endsWith(".bsl") || lower.endsWith(".os")) { //$NON-NLS-1$ //$NON-NLS-2$
                return true;
            }
        }
        // Проверяем по имени файла
        if (fileName != null) {
            String lower = fileName.toLowerCase();
            if (lower.endsWith(".bsl") || lower.endsWith(".os")) { //$NON-NLS-1$ //$NON-NLS-2$
                return true;
            }
            // Также проверяем типичные имена модулей 1C без расширения
            if (lower.contains("module") || lower.contains("модуль") //$NON-NLS-1$ //$NON-NLS-2$
                    || lower.contains("клиент") || lower.contains("сервер") //$NON-NLS-1$ //$NON-NLS-2$
                    || lower.contains("client") || lower.contains("server") //$NON-NLS-1$ //$NON-NLS-2$
                    || lower.contains("вызов") || lower.contains("менеджер") //$NON-NLS-1$ //$NON-NLS-2$
                    || lower.contains("объект") || lower.contains("форм")) { //$NON-NLS-1$ //$NON-NLS-2$
                return true;
            }
        }
        return false;
    }

    /**
     * Получает окружающий код вокруг выделения.
     */
    private String getSurroundingCode(IDocument document, int startLine, int endLine) throws BadLocationException {
        int contextBefore = MAX_CONTEXT_LINES / 2;
        int contextAfter = MAX_CONTEXT_LINES / 2;

        int docLines = document.getNumberOfLines();
        int fromLine = Math.max(0, startLine - contextBefore);
        int toLine = Math.min(docLines - 1, endLine + contextAfter);

        int startOffset = document.getLineOffset(fromLine);
        int endOffset = document.getLineOffset(toLine) + document.getLineLength(toLine);

        String code = document.get(startOffset, endOffset - startOffset);

        // Ограничиваем размер
        if (code.length() > MAX_DOCUMENT_CHARS) {
            code = code.substring(0, MAX_DOCUMENT_CHARS) + "\n... (обрезано)"; //$NON-NLS-1$
        }

        return code;
    }

    /**
     * Определяет тип модуля по пути файла.
     */
    private String detectModuleType(String path) {
        if (path == null) return ""; //$NON-NLS-1$
        String lowerPath = path.toLowerCase();

        if (lowerPath.contains("/commonmodules/")) return "ОбщийМодуль"; //$NON-NLS-1$ //$NON-NLS-2$
        if (lowerPath.contains("/forms/")) return "МодульФормы"; //$NON-NLS-1$ //$NON-NLS-2$
        if (lowerPath.contains("/commands/")) return "МодульКоманды"; //$NON-NLS-1$ //$NON-NLS-2$
        if (lowerPath.contains("objectmodule")) return "МодульОбъекта"; //$NON-NLS-1$ //$NON-NLS-2$
        if (lowerPath.contains("managermodule")) return "МодульМенеджера"; //$NON-NLS-1$ //$NON-NLS-2$
        if (lowerPath.contains("recordsetmodule")) return "МодульНабораЗаписей"; //$NON-NLS-1$ //$NON-NLS-2$

        return "Модуль"; //$NON-NLS-1$
    }

    private String getSelectedText(IWorkbenchPage page) {
        IEditorPart editor = page.getActiveEditor();
        LOG.info("getSelectedText: редактор=%s", editor != null ? editor.getClass().getName() : "null"); //$NON-NLS-1$ //$NON-NLS-2$

        if (editor == null) {
            LOG.warn("getSelectedText: редактор = null"); //$NON-NLS-1$
            return null;
        }

        // Логируем все интерфейсы редактора
        Class<?> editorClass = editor.getClass();
        LOG.info("getSelectedText: класс редактора: %s", editorClass.getName()); //$NON-NLS-1$
        LOG.info("getSelectedText: суперкласс: %s", editorClass.getSuperclass() != null ? editorClass.getSuperclass().getName() : "null"); //$NON-NLS-1$ //$NON-NLS-2$
        for (Class<?> iface : editorClass.getInterfaces()) {
            LOG.info("getSelectedText: интерфейс: %s", iface.getName()); //$NON-NLS-1$
        }

        // Попробуем несколько способов получить ITextEditor
        ITextEditor textEditor = null;

        // Способ 1: прямая проверка
        if (editor instanceof ITextEditor) {
            textEditor = (ITextEditor) editor;
            LOG.info("getSelectedText: редактор является ITextEditor напрямую"); //$NON-NLS-1$
        } else {
            LOG.info("getSelectedText: редактор НЕ является ITextEditor"); //$NON-NLS-1$
        }

        // Способ 2: через адаптер
        if (textEditor == null && editor instanceof IAdaptable) {
            LOG.info("getSelectedText: пробуем адаптер ITextEditor..."); //$NON-NLS-1$
            textEditor = ((IAdaptable) editor).getAdapter(ITextEditor.class);
            LOG.info("getSelectedText: адаптер вернул %s", textEditor != null ? "ITextEditor" : "null"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }

        // Способ 3: получить selection напрямую из редактора
        if (textEditor == null) {
            LOG.info("getSelectedText: пробуем получить selection напрямую из редактора"); //$NON-NLS-1$
            ISelectionProvider selectionProvider = editor.getSite().getSelectionProvider();
            LOG.info("getSelectedText: selectionProvider=%s", //$NON-NLS-1$
                    selectionProvider != null ? selectionProvider.getClass().getName() : "null"); //$NON-NLS-1$
            if (selectionProvider != null) {
                ISelection selection = selectionProvider.getSelection();
                LOG.info("getSelectedText: selection тип=%s, isEmpty=%s", //$NON-NLS-1$
                        selection != null ? selection.getClass().getName() : "null", //$NON-NLS-1$
                        selection != null ? selection.isEmpty() : "N/A"); //$NON-NLS-1$
                if (selection instanceof ITextSelection textSelection) {
                    String text = textSelection.getText();
                    LOG.info("getSelectedText: получен текст длиной %d через getSite()", //$NON-NLS-1$
                            text != null ? text.length() : 0);
                    return text;
                } else {
                    LOG.info("getSelectedText: selection НЕ является ITextSelection"); //$NON-NLS-1$
                }
            }
        }

        // Способ 4: через ISourceViewer (для Xtext/1C EDT редакторов)
        if (textEditor == null) {
            LOG.info("getSelectedText: пробуем через ISourceViewer"); //$NON-NLS-1$
            ISourceViewer sourceViewer = getSourceViewer(editor);
            if (sourceViewer != null) {
                LOG.info("getSelectedText: sourceViewer получен: %s", sourceViewer.getClass().getName()); //$NON-NLS-1$
                Point selection = sourceViewer.getSelectedRange();
                LOG.info("getSelectedText: selectedRange offset=%d, length=%d", selection.x, selection.y); //$NON-NLS-1$
                if (selection.y > 0) {
                    IDocument document = sourceViewer.getDocument();
                    if (document != null) {
                        try {
                            String text = document.get(selection.x, selection.y);
                            LOG.info("getSelectedText: получен текст длиной %d через ISourceViewer", text.length()); //$NON-NLS-1$
                            return text;
                        } catch (Exception e) {
                            LOG.error("getSelectedText: ошибка получения текста: %s", e.getMessage()); //$NON-NLS-1$
                        }
                    }
                } else {
                    LOG.info("getSelectedText: нет выделения (length=0)"); //$NON-NLS-1$
                }
            }
        }

        // Способ 5: через StyledText виджет напрямую
        if (textEditor == null) {
            LOG.info("getSelectedText: пробуем через StyledText"); //$NON-NLS-1$
            ISourceViewer sourceViewer = getSourceViewer(editor);
            if (sourceViewer != null) {
                StyledText textWidget = sourceViewer.getTextWidget();
                if (textWidget != null && !textWidget.isDisposed()) {
                    String selectedText = textWidget.getSelectionText();
                    LOG.info("getSelectedText: StyledText selectionText длиной %d", //$NON-NLS-1$
                            selectedText != null ? selectedText.length() : 0);
                    if (selectedText != null && !selectedText.isEmpty()) {
                        return selectedText;
                    }
                }
            }
        }

        // Если получили ITextEditor - используем его
        if (textEditor != null) {
            ISelection selection = textEditor.getSelectionProvider().getSelection();
            LOG.info("getSelectedText: ITextEditor selection=%s", selection != null ? selection.getClass().getName() : "null"); //$NON-NLS-1$ //$NON-NLS-2$

            if (selection instanceof ITextSelection textSelection) {
                String text = textSelection.getText();
                LOG.info("getSelectedText: длина текста=%d", text != null ? text.length() : 0); //$NON-NLS-1$
                return text;
            }
        }

        LOG.warn("getSelectedText: не удалось получить выделенный текст"); //$NON-NLS-1$
        return null;
    }

    /**
     * Получает ISourceViewer из редактора через адаптер или рефлексию.
     */
    private ISourceViewer getSourceViewer(IEditorPart editor) {
        // Способ 1: через адаптер
        if (editor instanceof IAdaptable) {
            Object adapter = ((IAdaptable) editor).getAdapter(ISourceViewer.class);
            if (adapter instanceof ISourceViewer) {
                LOG.info("getSourceViewer: получен через адаптер"); //$NON-NLS-1$
                return (ISourceViewer) adapter;
            }
        }

        // Способ 2: через рефлексию (многие редакторы имеют метод getSourceViewer())
        try {
            java.lang.reflect.Method method = editor.getClass().getMethod("getSourceViewer"); //$NON-NLS-1$
            method.setAccessible(true);
            Object result = method.invoke(editor);
            if (result instanceof ISourceViewer) {
                LOG.info("getSourceViewer: получен через рефлексию getSourceViewer()"); //$NON-NLS-1$
                return (ISourceViewer) result;
            }
        } catch (Exception e) {
            LOG.info("getSourceViewer: метод getSourceViewer() не найден: %s", e.getMessage()); //$NON-NLS-1$
        }

        // Способ 3: поиск поля sourceViewer через рефлексию
        try {
            Class<?> clazz = editor.getClass();
            while (clazz != null) {
                for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
                    if (ISourceViewer.class.isAssignableFrom(field.getType())) {
                        field.setAccessible(true);
                        Object value = field.get(editor);
                        if (value instanceof ISourceViewer) {
                            LOG.info("getSourceViewer: получен через поле %s", field.getName()); //$NON-NLS-1$
                            return (ISourceViewer) value;
                        }
                    }
                }
                clazz = clazz.getSuperclass();
            }
        } catch (Exception e) {
            LOG.info("getSourceViewer: поиск поля не удался: %s", e.getMessage()); //$NON-NLS-1$
        }

        LOG.warn("getSourceViewer: не удалось получить ISourceViewer"); //$NON-NLS-1$
        return null;
    }

    /**
     * Строит промпт с полным контекстом для LLM.
     */
    protected String buildExplainPromptWithContext(EditorContext context) {
        StringBuilder sb = new StringBuilder();

        sb.append("Объясни следующий код на языке 1С.\n\n"); //$NON-NLS-1$

        // Информация о файле
        if (context.fileName != null && !context.fileName.isEmpty()) {
            sb.append("**Файл:** ").append(context.fileName); //$NON-NLS-1$
            if (context.moduleType != null && !context.moduleType.isEmpty()) {
                sb.append(" (").append(context.moduleType).append(")"); //$NON-NLS-1$ //$NON-NLS-2$
            }
            sb.append("\n"); //$NON-NLS-1$
        }

        // Позиция в файле
        if (context.selectionStartLine >= 0) {
            // Позиция в файле (если вне метода)
            sb.append("**Строки:** ").append(context.selectionStartLine + 1); //$NON-NLS-1$
            if (context.selectionEndLine != context.selectionStartLine) {
                sb.append("-").append(context.selectionEndLine + 1); //$NON-NLS-1$
            }
            sb.append(" из ").append(context.totalLines).append("\n"); //$NON-NLS-1$ //$NON-NLS-2$
        }

        sb.append("\n"); //$NON-NLS-1$

        // Выделенный код (основной для анализа)
        sb.append("## Анализируемый код:\n"); //$NON-NLS-1$
        sb.append("```bsl\n").append(context.selectedText).append("\n```\n\n"); //$NON-NLS-1$ //$NON-NLS-2$

        // Окружающий контекст
        if (context.surroundingCode != null && !context.surroundingCode.isEmpty()
                && !context.surroundingCode.equals(context.selectedText)) {
            sb.append("## Контекст (окружающий код):\n"); //$NON-NLS-1$
            sb.append("```bsl\n").append(context.surroundingCode).append("\n```\n\n"); //$NON-NLS-1$ //$NON-NLS-2$
        }

        sb.append("Пожалуйста, объясни:\n"); //$NON-NLS-1$
        sb.append("1. Что делает этот код\n"); //$NON-NLS-1$
        sb.append("2. Как он работает\n"); //$NON-NLS-1$
        sb.append("3. Возможные улучшения (если есть)\n"); //$NON-NLS-1$

        return sb.toString();
    }

    private String buildExplainPrompt(String code) {
        return String.format("""
            Объясни следующий код на языке 1С:

            ```bsl
            %s
            ```

            Пожалуйста, объясни:
            1. Что делает этот код
            2. Как он работает
            3. Возможные улучшения (если есть)
            """, code); //$NON-NLS-1$
    }
}
