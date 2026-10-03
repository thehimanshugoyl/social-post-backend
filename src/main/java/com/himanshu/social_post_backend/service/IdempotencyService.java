package com.himanshu.social_post_backend.service;

import com.himanshu.social_post_backend.model.ProcessedEvent;
import java.util.Optional;

/**
 * Service managing event idempotency to guarantee that duplicate messages
 * arriving via Kafka's at-least-once delivery semantics are detected and deduplicated.
 */
public interface IdempotencyService {

    boolean isEventProcessed(String eventId);

    ProcessedEvent registerEventReceived(String eventId, String eventType, String aggregateId, String correlationId);

    void markEventSuccess(String eventId, String aggregateId, String details);

    void markEventDuplicateSkipped(String eventId);

    void markEventDlq(String eventId, String failureReason);

    Optional<ProcessedEvent> getEvent(String eventId);
}
