package com.himanshu.social_post_backend.service.impl;

import com.himanshu.social_post_backend.model.ProcessedEvent;
import com.himanshu.social_post_backend.model.ProcessedEventStatus;
import com.himanshu.social_post_backend.repository.ProcessedEventRepository;
import com.himanshu.social_post_backend.service.IdempotencyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class IdempotencyServiceImpl implements IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyServiceImpl.class);

    private final ProcessedEventRepository processedEventRepository;

    public IdempotencyServiceImpl(ProcessedEventRepository processedEventRepository) {
        this.processedEventRepository = processedEventRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEventProcessed(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            return false;
        }
        Optional<ProcessedEvent> eventOpt = processedEventRepository.findByEventId(eventId);
        if (eventOpt.isPresent()) {
            ProcessedEventStatus status = eventOpt.get().getStatus();
            boolean isCompleted = (status == ProcessedEventStatus.PROCESSED || status == ProcessedEventStatus.DUPLICATE_SKIPPED);
            if (isCompleted) {
                log.warn("[Idempotency] Duplicate event detected for eventId: '{}'. Status is already '{}'", eventId, status);
            }
            return isCompleted;
        }
        return false;
    }

    @Override
    @Transactional
    public boolean tryClaimEventProcessing(String eventId, String eventType, String correlationId) {
        if (eventId == null || eventId.isBlank()) {
            return false;
        }
        if (processedEventRepository.existsByEventId(eventId)) {
            markEventDuplicateSkipped(eventId);
            return false;
        }
        try {
            ProcessedEvent newEvent = new ProcessedEvent(
                    eventId,
                    eventType != null ? eventType : "SCHEDULED_POST",
                    null,
                    ProcessedEventStatus.PROCESSED,
                    correlationId
            );
            processedEventRepository.saveAndFlush(newEvent);
            return true;
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            log.warn("[Idempotency] Concurrent race condition safely intercepted for eventId: '{}'. Skipping duplicate.", eventId);
            markEventDuplicateSkipped(eventId);
            return false;
        }
    }

    @Override
    @Transactional
    public ProcessedEvent registerEventReceived(String eventId, String eventType, String aggregateId, String correlationId) {
        return processedEventRepository.findByEventId(eventId)
                .map(existing -> {
                    existing.incrementAttempts();
                    log.info("[Idempotency] Re-attempt {} for eventId: '{}'", existing.getAttemptsCount(), eventId);
                    return processedEventRepository.save(existing);
                })
                .orElseGet(() -> {
                    ProcessedEvent newEvent = new ProcessedEvent(
                            eventId,
                            eventType,
                            aggregateId,
                            ProcessedEventStatus.PROCESSED,
                            correlationId
                    );
                    log.info("[Idempotency] Registering new event for tracking. eventId: '{}', type: '{}'", eventId, eventType);
                    return processedEventRepository.save(newEvent);
                });
    }

    @Override
    @Transactional
    public void markEventSuccess(String eventId, String aggregateId, String details) {
        processedEventRepository.findByEventId(eventId).ifPresentOrElse(event -> {
            event.setStatus(ProcessedEventStatus.PROCESSED);
            event.setAggregateId(aggregateId);
            event.setProcessedAt(Instant.now());
            event.setDetails(details);
            processedEventRepository.save(event);
            log.info("[Idempotency] Marked eventId: '{}' as successfully PROCESSED. aggregateId: '{}'", eventId, aggregateId);
        }, () -> {
            ProcessedEvent event = new ProcessedEvent(
                    eventId,
                    "SCHEDULED_POST",
                    aggregateId,
                    ProcessedEventStatus.PROCESSED,
                    null
            );
            event.setDetails(details);
            processedEventRepository.save(event);
            log.info("[Idempotency] Created and marked eventId: '{}' as PROCESSED.", eventId);
        });
    }

    @Override
    @Transactional
    public void markEventDuplicateSkipped(String eventId) {
        processedEventRepository.findByEventId(eventId).ifPresent(event -> {
            event.incrementAttempts();
            event.setDetails("Duplicate message arrived - skipped without reprocessing.");
            processedEventRepository.save(event);
            log.info("[Idempotency] Logged duplicate skip for eventId: '{}'. Total arrivals: {}", eventId, event.getAttemptsCount());
        });
    }

    @Override
    @Transactional
    public void markEventDlq(String eventId, String failureReason) {
        processedEventRepository.findByEventId(eventId).ifPresentOrElse(event -> {
            event.setStatus(ProcessedEventStatus.FAILED_DLQ);
            event.setDetails("Routed to DLQ: " + failureReason);
            processedEventRepository.save(event);
            log.warn("[Idempotency] Marked eventId: '{}' as FAILED_DLQ. Reason: {}", eventId, failureReason);
        }, () -> {
            ProcessedEvent event = new ProcessedEvent(
                    eventId,
                    "SCHEDULED_POST",
                    null,
                    ProcessedEventStatus.FAILED_DLQ,
                    null
            );
            event.setDetails("Routed to DLQ: " + failureReason);
            processedEventRepository.save(event);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProcessedEvent> getEvent(String eventId) {
        return processedEventRepository.findByEventId(eventId);
    }
}
