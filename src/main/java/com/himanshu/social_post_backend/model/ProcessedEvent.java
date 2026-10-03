package com.himanshu.social_post_backend.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

/**
 * Entity for tracking Kafka events to ensure Idempotency.
 * Storing unique event IDs guarantees at-least-once Kafka messages
 * are not processed multiple times, preventing duplicate posts or inconsistent state.
 */
@Entity
@Table(
        name = "processed_events",
        indexes = {
                @Index(name = "idx_processed_event_status", columnList = "status"),
                @Index(name = "idx_processed_event_type", columnList = "eventType")
        }
)
public class ProcessedEvent {

    @Id
    @Column(name = "event_id", length = 64, nullable = false, updatable = false)
    private String eventId;

    @Column(name = "event_type", length = 50, nullable = false)
    private String eventType;

    @Column(name = "aggregate_id", length = 100)
    private String aggregateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private ProcessedEventStatus status;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "attempts_count", nullable = false)
    private int attemptsCount = 1;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @Column(name = "details", length = 500)
    private String details;

    public ProcessedEvent() {
    }

    public ProcessedEvent(String eventId, String eventType, String aggregateId, ProcessedEventStatus status, String correlationId) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.aggregateId = aggregateId;
        this.status = status;
        this.correlationId = correlationId;
        this.receivedAt = Instant.now();
        this.processedAt = Instant.now();
        this.attemptsCount = 1;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(String aggregateId) {
        this.aggregateId = aggregateId;
    }

    public ProcessedEventStatus getStatus() {
        return status;
    }

    public void setStatus(ProcessedEventStatus status) {
        this.status = status;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }

    public int getAttemptsCount() {
        return attemptsCount;
    }

    public void setAttemptsCount(int attemptsCount) {
        this.attemptsCount = attemptsCount;
    }

    public void incrementAttempts() {
        this.attemptsCount++;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProcessedEvent that = (ProcessedEvent) o;
        return Objects.equals(eventId, that.eventId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId);
    }
}
