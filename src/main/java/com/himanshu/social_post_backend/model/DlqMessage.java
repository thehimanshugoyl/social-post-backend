package com.himanshu.social_post_backend.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Entity for persisting failed Kafka events routed to the Dead Letter Queue (DLQ).
 * Captures the event payload, exception diagnosis, failure timestamps, and retry counts
 * for post-mortem analysis and dead-letter replay recovery.
 */
@Entity
@Table(
        name = "dlq_messages",
        indexes = {
                @Index(name = "idx_dlq_event_id", columnList = "eventId"),
                @Index(name = "idx_dlq_resolved", columnList = "resolved"),
                @Index(name = "idx_dlq_failed_at", columnList = "failureTimestamp")
        }
)
public class DlqMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", length = 64)
    private String eventId;

    @Column(name = "original_topic", length = 100, nullable = false)
    private String originalTopic;

    @Column(name = "dlq_topic", length = 100, nullable = false)
    private String dlqTopic;

    @Column(name = "payload", columnDefinition = "TEXT", nullable = false)
    private String payload;

    @Column(name = "exception_class", length = 200)
    private String exceptionClass;

    @Column(name = "exception_message", columnDefinition = "TEXT")
    private String exceptionMessage;

    @Column(name = "failure_timestamp", nullable = false)
    private Instant failureTimestamp;

    @Column(name = "retry_attempts", nullable = false)
    private int retryAttempts = 0;

    @Column(name = "resolved", nullable = false)
    private boolean resolved = false;

    @Column(name = "resolution_notes", length = 500)
    private String resolutionNotes;

    public DlqMessage() {
    }

    public DlqMessage(String eventId, String originalTopic, String dlqTopic, String payload,
                      String exceptionClass, String exceptionMessage, int retryAttempts) {
        this.eventId = eventId;
        this.originalTopic = originalTopic;
        this.dlqTopic = dlqTopic;
        this.payload = payload;
        this.exceptionClass = exceptionClass;
        this.exceptionMessage = exceptionMessage;
        this.retryAttempts = retryAttempts;
        this.failureTimestamp = Instant.now();
        this.resolved = false;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getOriginalTopic() {
        return originalTopic;
    }

    public void setOriginalTopic(String originalTopic) {
        this.originalTopic = originalTopic;
    }

    public String getDlqTopic() {
        return dlqTopic;
    }

    public void setDlqTopic(String dlqTopic) {
        this.dlqTopic = dlqTopic;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getExceptionClass() {
        return exceptionClass;
    }

    public void setExceptionClass(String exceptionClass) {
        this.exceptionClass = exceptionClass;
    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }

    public void setExceptionMessage(String exceptionMessage) {
        this.exceptionMessage = exceptionMessage;
    }

    public Instant getFailureTimestamp() {
        return failureTimestamp;
    }

    public void setFailureTimestamp(Instant failureTimestamp) {
        this.failureTimestamp = failureTimestamp;
    }

    public int getRetryAttempts() {
        return retryAttempts;
    }

    public void setRetryAttempts(int retryAttempts) {
        this.retryAttempts = retryAttempts;
    }

    public boolean isResolved() {
        return resolved;
    }

    public void setResolved(boolean resolved) {
        this.resolved = resolved;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public void setResolutionNotes(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
    }
}
