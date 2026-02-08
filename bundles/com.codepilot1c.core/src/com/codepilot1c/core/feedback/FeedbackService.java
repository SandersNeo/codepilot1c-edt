/*
 * Copyright (c) 2024 Example
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0
 */
package com.codepilot1c.core.feedback;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.codepilot1c.core.internal.VibeCorePlugin;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

/**
 * Implementation of the feedback service.
 *
 * <p>Currently stores feedback locally for analytics. In a future version,
 * this could send feedback to a backend service for model improvement.</p>
 *
 * <p>All operations are asynchronous and fire-and-forget to avoid
 * impacting user experience.</p>
 */
public class FeedbackService implements IFeedbackService {

    private static FeedbackService instance;

    private final ScheduledExecutorService executor;
    private final Gson gson;
    private final Map<String, FeedbackRecord> feedbackHistory;

    /** Maximum feedback records to keep in memory */
    private static final int MAX_HISTORY_SIZE = 1000;

    private FeedbackService() {
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Vibe-Feedback"); //$NON-NLS-1$
            t.setDaemon(true);
            return t;
        });
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.feedbackHistory = new ConcurrentHashMap<>();
    }

    /**
     * Returns the singleton instance.
     *
     * @return the feedback service
     */
    public static synchronized FeedbackService getInstance() {
        if (instance == null) {
            instance = new FeedbackService();
        }
        return instance;
    }

    @Override
    public CompletableFuture<Void> completionAccepted(String sessionId, String acceptedCode,
            int cursorOffsetBefore, int cursorOffsetAfter) {

        return CompletableFuture.runAsync(() -> {
            JsonObject data = new JsonObject();
            data.addProperty("type", FeedbackType.COMPLETION_ACCEPTED.getId()); //$NON-NLS-1$
            data.addProperty("sessionId", sessionId); //$NON-NLS-1$
            data.addProperty("codeLength", acceptedCode != null ? acceptedCode.length() : 0); //$NON-NLS-1$
            data.addProperty("cursorBefore", cursorOffsetBefore); //$NON-NLS-1$
            data.addProperty("cursorAfter", cursorOffsetAfter); //$NON-NLS-1$

            recordFeedback(sessionId, FeedbackType.COMPLETION_ACCEPTED, data);
            VibeCorePlugin.logInfo("Feedback: completion accepted, session=" + sessionId //$NON-NLS-1$
                    + ", codeLen=" + (acceptedCode != null ? acceptedCode.length() : 0)); //$NON-NLS-1$
        }, executor);
    }

    @Override
    public CompletableFuture<Void> completionDismissed(String sessionId, String dismissedCode, String reason) {

        return CompletableFuture.runAsync(() -> {
            JsonObject data = new JsonObject();
            data.addProperty("type", FeedbackType.COMPLETION_DISMISSED.getId()); //$NON-NLS-1$
            data.addProperty("sessionId", sessionId); //$NON-NLS-1$
            data.addProperty("codeLength", dismissedCode != null ? dismissedCode.length() : 0); //$NON-NLS-1$
            if (reason != null) {
                data.addProperty("reason", reason); //$NON-NLS-1$
            }

            recordFeedback(sessionId, FeedbackType.COMPLETION_DISMISSED, data);
            VibeCorePlugin.logInfo("Feedback: completion dismissed, session=" + sessionId); //$NON-NLS-1$
        }, executor);
    }

    @Override
    public CompletableFuture<Void> codeFinalized(String sessionId, String finalCode) {

        return CompletableFuture.runAsync(() -> {
            JsonObject data = new JsonObject();
            data.addProperty("type", FeedbackType.CODE_FINALIZED.getId()); //$NON-NLS-1$
            data.addProperty("sessionId", sessionId); //$NON-NLS-1$
            data.addProperty("codeLength", finalCode != null ? finalCode.length() : 0); //$NON-NLS-1$

            recordFeedback(sessionId, FeedbackType.CODE_FINALIZED, data);
            VibeCorePlugin.logInfo("Feedback: code finalized, session=" + sessionId); //$NON-NLS-1$
        }, executor);
    }

    @Override
    public CompletableFuture<Void> codeModified(String sessionId, String originalCode, String modifiedCode) {

        return CompletableFuture.runAsync(() -> {
            JsonObject data = new JsonObject();
            data.addProperty("type", FeedbackType.CODE_MODIFIED.getId()); //$NON-NLS-1$
            data.addProperty("sessionId", sessionId); //$NON-NLS-1$
            data.addProperty("originalLength", originalCode != null ? originalCode.length() : 0); //$NON-NLS-1$
            data.addProperty("modifiedLength", modifiedCode != null ? modifiedCode.length() : 0); //$NON-NLS-1$

            // Calculate similarity (simple Levenshtein distance ratio could be added)
            boolean significantChange = originalCode != null && modifiedCode != null
                    && Math.abs(originalCode.length() - modifiedCode.length()) > originalCode.length() / 2;
            data.addProperty("significantChange", significantChange); //$NON-NLS-1$

            recordFeedback(sessionId, FeedbackType.CODE_MODIFIED, data);
            VibeCorePlugin.logInfo("Feedback: code modified, session=" + sessionId //$NON-NLS-1$
                    + ", significant=" + significantChange); //$NON-NLS-1$
        }, executor);
    }

    @Override
    public CompletableFuture<Void> reportIssue(String sessionId, IssueType issueType,
            String description, String context) {

        return CompletableFuture.runAsync(() -> {
            JsonObject data = new JsonObject();
            data.addProperty("type", FeedbackType.ISSUE_REPORTED.getId()); //$NON-NLS-1$
            data.addProperty("sessionId", sessionId); //$NON-NLS-1$
            data.addProperty("issueType", issueType.getId()); //$NON-NLS-1$
            if (description != null) {
                data.addProperty("description", description); //$NON-NLS-1$
            }
            if (context != null) {
                data.addProperty("contextLength", context.length()); //$NON-NLS-1$
            }

            recordFeedback(sessionId, FeedbackType.ISSUE_REPORTED, data);
            VibeCorePlugin.logInfo("Feedback: issue reported, session=" + sessionId //$NON-NLS-1$
                    + ", type=" + issueType.getId()); //$NON-NLS-1$
        }, executor);
    }

    @Override
    public CompletableFuture<Void> sendFeedback(String sessionId, FeedbackType type, String metadata) {

        return CompletableFuture.runAsync(() -> {
            JsonObject data = new JsonObject();
            data.addProperty("type", type.getId()); //$NON-NLS-1$
            data.addProperty("sessionId", sessionId); //$NON-NLS-1$
            if (metadata != null) {
                data.addProperty("metadata", metadata); //$NON-NLS-1$
            }

            recordFeedback(sessionId, type, data);
            VibeCorePlugin.logInfo("Feedback: " + type.getId() + ", session=" + sessionId); //$NON-NLS-1$ //$NON-NLS-2$
        }, executor);
    }

    /**
     * Records feedback in the local history.
     */
    private void recordFeedback(String sessionId, FeedbackType type, JsonObject data) {
        // Trim history if needed
        if (feedbackHistory.size() >= MAX_HISTORY_SIZE) {
            // Remove oldest entries (simple approach)
            feedbackHistory.keySet().stream()
                    .limit(MAX_HISTORY_SIZE / 10)
                    .forEach(feedbackHistory::remove);
        }

        String key = sessionId + "_" + type.getId() + "_" + System.currentTimeMillis(); //$NON-NLS-1$ //$NON-NLS-2$
        feedbackHistory.put(key, new FeedbackRecord(sessionId, type, Instant.now(), data));
    }

    /**
     * Returns feedback statistics for analytics.
     *
     * @return statistics map
     */
    public Map<FeedbackType, Long> getStatistics() {
        Map<FeedbackType, Long> stats = new ConcurrentHashMap<>();
        for (FeedbackType type : FeedbackType.values()) {
            stats.put(type, 0L);
        }
        feedbackHistory.values().forEach(record ->
                stats.merge(record.type, 1L, Long::sum));
        return stats;
    }

    /**
     * Returns the acceptance rate (accepted / (accepted + dismissed)).
     *
     * @return acceptance rate between 0.0 and 1.0, or -1 if no data
     */
    public double getAcceptanceRate() {
        long accepted = feedbackHistory.values().stream()
                .filter(r -> r.type == FeedbackType.COMPLETION_ACCEPTED)
                .count();
        long dismissed = feedbackHistory.values().stream()
                .filter(r -> r.type == FeedbackType.COMPLETION_DISMISSED)
                .count();
        long total = accepted + dismissed;
        return total > 0 ? (double) accepted / total : -1;
    }

    /**
     * Clears the feedback history.
     */
    public void clearHistory() {
        feedbackHistory.clear();
    }

    /**
     * Disposes the service and releases resources.
     */
    public void dispose() {
        executor.shutdown();
        feedbackHistory.clear();
    }

    /**
     * Internal record for storing feedback.
     */
    private static class FeedbackRecord {
        final String sessionId;
        final FeedbackType type;
        final Instant timestamp;
        final JsonObject data;

        FeedbackRecord(String sessionId, FeedbackType type, Instant timestamp, JsonObject data) {
            this.sessionId = sessionId;
            this.type = type;
            this.timestamp = timestamp;
            this.data = data;
        }
    }
}
