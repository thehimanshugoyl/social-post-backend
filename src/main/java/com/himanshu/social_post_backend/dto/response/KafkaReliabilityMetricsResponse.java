package com.himanshu.social_post_backend.dto.response;

import java.time.Instant;

/**
 * Diagnostic metrics snapshot evaluating system reliability for Experiment 3.1.2 (CO5 - BT5, CO6 - BT6).
 */
public class KafkaReliabilityMetricsResponse {

    private long totalEventsProcessed;
    private long totalDuplicatesSkipped;
    private long totalRetriesAttempted;
    private long totalDlqMessagesRecorded;
    private long totalDlqMessagesResolved;
    private long totalDlqMessagesUnresolved;
    private String systemHealth;
    private Instant timestamp;

    public KafkaReliabilityMetricsResponse() {
        this.timestamp = Instant.now();
    }

    public KafkaReliabilityMetricsResponse(long totalEventsProcessed,
                                          long totalDuplicatesSkipped,
                                          long totalRetriesAttempted,
                                          long totalDlqMessagesRecorded,
                                          long totalDlqMessagesResolved,
                                          long totalDlqMessagesUnresolved,
                                          String systemHealth) {
        this.totalEventsProcessed = totalEventsProcessed;
        this.totalDuplicatesSkipped = totalDuplicatesSkipped;
        this.totalRetriesAttempted = totalRetriesAttempted;
        this.totalDlqMessagesRecorded = totalDlqMessagesRecorded;
        this.totalDlqMessagesResolved = totalDlqMessagesResolved;
        this.totalDlqMessagesUnresolved = totalDlqMessagesUnresolved;
        this.systemHealth = systemHealth;
        this.timestamp = Instant.now();
    }

    public long getTotalEventsProcessed() {
        return totalEventsProcessed;
    }

    public void setTotalEventsProcessed(long totalEventsProcessed) {
        this.totalEventsProcessed = totalEventsProcessed;
    }

    public long getTotalDuplicatesSkipped() {
        return totalDuplicatesSkipped;
    }

    public void setTotalDuplicatesSkipped(long totalDuplicatesSkipped) {
        this.totalDuplicatesSkipped = totalDuplicatesSkipped;
    }

    public long getTotalRetriesAttempted() {
        return totalRetriesAttempted;
    }

    public void setTotalRetriesAttempted(long totalRetriesAttempted) {
        this.totalRetriesAttempted = totalRetriesAttempted;
    }

    public long getTotalDlqMessagesRecorded() {
        return totalDlqMessagesRecorded;
    }

    public void setTotalDlqMessagesRecorded(long totalDlqMessagesRecorded) {
        this.totalDlqMessagesRecorded = totalDlqMessagesRecorded;
    }

    public long getTotalDlqMessagesResolved() {
        return totalDlqMessagesResolved;
    }

    public void setTotalDlqMessagesResolved(long totalDlqMessagesResolved) {
        this.totalDlqMessagesResolved = totalDlqMessagesResolved;
    }

    public long getTotalDlqMessagesUnresolved() {
        return totalDlqMessagesUnresolved;
    }

    public void setTotalDlqMessagesUnresolved(long totalDlqMessagesUnresolved) {
        this.totalDlqMessagesUnresolved = totalDlqMessagesUnresolved;
    }

    public String getSystemHealth() {
        return systemHealth;
    }

    public void setSystemHealth(String systemHealth) {
        this.systemHealth = systemHealth;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
