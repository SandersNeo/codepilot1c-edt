/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
 */
package com.codepilot1c.ui.handlers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.MarkerAnnotation;

import com.codepilot1c.core.logging.LogSanitizer;
import com.codepilot1c.core.logging.VibeLogger;

/**
 * Debug command: dumps EDT diagnostics sources (markers + annotations) for the active editor.
 *
 * <p>Goal: identify which marker types / annotation types contain 1C EDT diagnostics and how
 * to read them reliably (saved vs unsaved editor state).</p>
 */
public class DumpEdtDiagnosticsHandler extends AbstractHandler {

    private static final VibeLogger.CategoryLogger LOG = VibeLogger.forClass(DumpEdtDiagnosticsHandler.class);

    private static final int MAX_ITEMS = 200;
    private static final int MAX_MESSAGE_LEN = 240;

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        IEditorPart editorPart = HandlerUtil.getActiveEditor(event);
        if (!(editorPart instanceof ITextEditor editor)) {
            MessageDialog.openInformation(
                    HandlerUtil.getActiveShell(event),
                    "Диагностики EDT",
                    "Активный редактор не является текстовым редактором.");
            return null;
        }

        IEditorInput input = editor.getEditorInput();
        IFile file = resolveFile(input);

        DumpResult dump = dumpEditorDiagnostics(editor, input, file);

        // Log full dump for copy/paste.
        LOG.info("=== EDT diagnostics dump ===\n%s", dump.fullLog);

        StringBuilder summary = new StringBuilder();
        summary.append("Файл: ").append(dump.fileLabel).append("\n");
        summary.append("Markers: ").append(dump.markerCount).append("\n");
        summary.append("Annotations: ").append(dump.annotationCount).append("\n\n");
        summary.append("Топ marker types:\n").append(formatTopMap(dump.markerTypes)).append("\n");
        summary.append("Топ annotation types:\n").append(formatTopMap(dump.annotationTypes)).append("\n");
        summary.append("\nПодробности записаны в лог:\n");
        summary.append(VibeLogger.getInstance().getLogFilePath());

        MessageDialog.openInformation(
                HandlerUtil.getActiveShell(event),
                "Диагностики EDT",
                summary.toString());

        return null;
    }

    private DumpResult dumpEditorDiagnostics(ITextEditor editor, IEditorInput input, IFile file) {
        String fileLabel = file != null ? file.getFullPath().toString() : String.valueOf(input);
        StringBuilder sb = new StringBuilder();
        sb.append("Active editor input: ").append(String.valueOf(input)).append("\n");
        sb.append("Resolved IFile: ").append(file != null ? file.getFullPath() : "null").append("\n\n");

        Map<String, Integer> markerTypes = new HashMap<>();
        Map<String, Integer> annotationTypes = new HashMap<>();

        List<String> markerSamples = new ArrayList<>();
        int markerCount = 0;

        if (file != null) {
            try {
                IMarker[] markers = file.findMarkers(null, true, IResource.DEPTH_ZERO);
                markerCount = Math.min(markers.length, MAX_ITEMS);
                sb.append("== Markers (showing up to ").append(MAX_ITEMS).append(") ==\n");
                for (int i = 0; i < markers.length && i < MAX_ITEMS; i++) {
                    IMarker m = markers[i];
                    String type = safeMarkerType(m);
                    markerTypes.merge(type, 1, Integer::sum);

                    String msg = String.valueOf(m.getAttribute(IMarker.MESSAGE, ""));
                    int line = m.getAttribute(IMarker.LINE_NUMBER, -1);
                    int severity = m.getAttribute(IMarker.SEVERITY, -1);
                    int charStart = m.getAttribute(IMarker.CHAR_START, -1);
                    int charEnd = m.getAttribute(IMarker.CHAR_END, -1);

                    String row = String.format(
                            "marker[%d]: type=%s, severity=%s, line=%d, char=%d..%d, message=%s",
                            i,
                            type,
                            formatSeverity(severity),
                            line,
                            charStart,
                            charEnd,
                            LogSanitizer.truncate(msg, MAX_MESSAGE_LEN));
                    sb.append(row).append("\n");

                    if (!msg.isBlank() && markerSamples.size() < 10) {
                        markerSamples.add(row);
                    }
                }
                sb.append("\n");
            } catch (CoreException e) {
                sb.append("Ошибка чтения markers: ").append(e.getMessage()).append("\n\n");
            }
        } else {
            sb.append("Markers skipped: IFile not resolved.\n\n");
        }

        int annotationCount = 0;
        List<String> annotationSamples = new ArrayList<>();

        IDocumentProvider docProvider = editor.getDocumentProvider();
        IDocument doc = docProvider != null ? docProvider.getDocument(input) : null;
        IAnnotationModel model = docProvider != null ? docProvider.getAnnotationModel(input) : null;

        if (model != null) {
            sb.append("== Annotations (showing up to ").append(MAX_ITEMS).append(") ==\n");

            Iterator<?> it = model.getAnnotationIterator();
            int idx = 0;
            while (it.hasNext() && idx < MAX_ITEMS) {
                Object obj = it.next();
                if (!(obj instanceof Annotation ann)) {
                    idx++;
                    continue;
                }

                String type = safeAnnotationType(ann);
                annotationTypes.merge(type, 1, Integer::sum);

                Position pos = model.getPosition(ann);
                int offset = pos != null ? pos.offset : -1;
                int length = pos != null ? pos.length : -1;
                int line = doc != null && offset >= 0 ? safeLineOfOffset(doc, offset) : -1;

                String text = Objects.toString(ann.getText(), "");

                String markerType = null;
                if (ann instanceof MarkerAnnotation markerAnn) {
                    IMarker marker = markerAnn.getMarker();
                    markerType = marker != null ? safeMarkerType(marker) : null;
                }

                String row = String.format(
                        "ann[%d]: type=%s, line=%d, offset=%d, len=%d, markerType=%s, text=%s",
                        idx,
                        type,
                        line,
                        offset,
                        length,
                        markerType != null ? markerType : "-",
                        LogSanitizer.truncate(text, MAX_MESSAGE_LEN));
                sb.append(row).append("\n");

                if ((!text.isBlank() || markerType != null) && annotationSamples.size() < 10) {
                    annotationSamples.add(row);
                }

                idx++;
            }

            annotationCount = idx;
            sb.append("\n");
        } else {
            sb.append("Annotations skipped: annotation model not available for this editor input.\n\n");
        }

        sb.append("== Summary ==\n");
        sb.append("File: ").append(fileLabel).append("\n");
        sb.append("Markers shown: ").append(markerCount).append("\n");
        sb.append("Annotations shown: ").append(annotationCount).append("\n\n");

        if (!markerSamples.isEmpty()) {
            sb.append("Marker samples:\n");
            for (String s : markerSamples) {
                sb.append("  ").append(s).append("\n");
            }
            sb.append("\n");
        }

        if (!annotationSamples.isEmpty()) {
            sb.append("Annotation samples:\n");
            for (String s : annotationSamples) {
                sb.append("  ").append(s).append("\n");
            }
            sb.append("\n");
        }

        sb.append("Marker type histogram:\n").append(formatHistogram(markerTypes)).append("\n");
        sb.append("Annotation type histogram:\n").append(formatHistogram(annotationTypes)).append("\n");

        return new DumpResult(fileLabel, markerCount, annotationCount, markerTypes, annotationTypes, sb.toString());
    }

    private IFile resolveFile(IEditorInput input) {
        if (input instanceof FileEditorInput fileInput) {
            return fileInput.getFile();
        }
        Object adapted = input.getAdapter(IFile.class);
        return adapted instanceof IFile ? (IFile) adapted : null;
    }

    private int safeLineOfOffset(IDocument doc, int offset) {
        try {
            return doc.getLineOfOffset(offset) + 1;
        } catch (Exception e) {
            return -1;
        }
    }

    private String safeMarkerType(IMarker marker) {
        try {
            return marker.getType();
        } catch (CoreException e) {
            return "unknown-marker-type";
        }
    }

    private String safeAnnotationType(Annotation ann) {
        try {
            return ann.getType();
        } catch (Exception e) {
            return ann.getClass().getName();
        }
    }

    private String formatSeverity(int sev) {
        return switch (sev) {
            case IMarker.SEVERITY_ERROR -> "ERROR";
            case IMarker.SEVERITY_WARNING -> "WARNING";
            case IMarker.SEVERITY_INFO -> "INFO";
            default -> String.valueOf(sev);
        };
    }

    private String formatTopMap(Map<String, Integer> map) {
        if (map.isEmpty()) {
            return "  (нет)\n";
        }
        return map.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                .limit(10)
                .map(e -> "  " + e.getKey() + " = " + e.getValue())
                .collect(Collectors.joining("\n")) + "\n";
    }

    private String formatHistogram(Map<String, Integer> map) {
        if (map.isEmpty()) {
            return "(нет)\n";
        }
        return map.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                .map(e -> e.getKey() + " = " + e.getValue())
                .collect(Collectors.joining("\n")) + "\n";
    }

    private record DumpResult(
            String fileLabel,
            int markerCount,
            int annotationCount,
            Map<String, Integer> markerTypes,
            Map<String, Integer> annotationTypes,
            String fullLog) {
    }
}

