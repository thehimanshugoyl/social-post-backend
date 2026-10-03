package com.himanshu.social_post_backend.consumer;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.himanshu.social_post_backend.model.DlqMessage;
import com.himanshu.social_post_backend.service.DlqManagementService;
import com.himanshu.social_post_backend.service.IdempotencyService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Dead Letter Queue (DLQ) Consumer for Experiment 3.1.1.
 * Listens to dead-lettered events routed by DeadLetterPublishingRecoverer after retry exhaustion.
 * Stores diagnostics, exception cause, and payload in dlq_messages table for administrative review and replay.
 */
@Component
public class ScheduledPostDlqConsumer {

    private static final Logger log = LoggerFactory.getLogger(ScheduledPostDlqConsumer.class);

    private final DlqManagementService dlqManagementService;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    private final AtomicInteger dlqReceivedCounter = new AtomicInteger(0);

    public ScheduledPostDlqConsumer(DlqManagementService dlqManagementService,
                                   IdempotencyService idempotencyService,
                                   ObjectMapper objectMapper) {
        this.dlqManagementService = dlqManagementService;
        this.idempotencyService = idempotencyService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.scheduled-posts-dlq:social-scheduled-posts.DLT}",
            groupId = "${spring.kafka.consumer.group-id:social-post-group}-dlq",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void consumeDlqMessage(ConsumerRecord<String, String> record) {
        dlqReceivedCounter.incrementAndGet();

        String payload = record.value();
        String originalTopic = extractHeader(record, KafkaHeaders.DLT_ORIGINAL_TOPIC, "scheduled-posts");
        String exceptionMessage = extractHeader(record, KafkaHeaders.DLT_EXCEPTION_MESSAGE, "Unknown error");
        String exceptionClass = extractHeader(record, KafkaHeaders.DLT_EXCEPTION_FQCN, "UnknownException");
        String stackTrace = extractHeader(record, KafkaHeaders.DLT_EXCEPTION_STACKTRACE, "");

        if (stackTrace.contains("FatalEventProcessingException") || exceptionMessage.contains("FatalEventProcessingException")) {
            exceptionClass = "FatalEventProcessingException";
        } else if (stackTrace.contains("TransientEventProcessingException") || exceptionMessage.contains("TransientEventProcessingException")) {
            exceptionClass = "TransientEventProcessingException";
        }

        String eventId = record.key();
        if (eventId == null || eventId.isBlank()) {
            try {
                JsonNode json = objectMapper.readTree(payload);
                if (json.has("eventId")) {
                    eventId = json.get("eventId").asText();
                }
            } catch (Exception ignored) {
            }
        }

        log.error("[DLQ-Consumer] Captured DEAD LETTER EVENT: eventId='{}', originalTopic='{}', dlqTopic='{}', exception='{}', message='{}'",
                eventId, originalTopic, record.topic(), exceptionClass, exceptionMessage);

        DlqMessage dlqMessage = new DlqMessage(
                eventId,
                originalTopic,
                record.topic(),
                payload,
                exceptionClass,
                exceptionMessage,
                3 // Exhausted retries
        );

        dlqManagementService.recordDlqMessage(dlqMessage);

        if (eventId != null) {
            idempotencyService.markEventDlq(eventId, exceptionMessage);
        }
    }

    private String extractHeader(ConsumerRecord<String, String> record, String headerKey, String defaultValue) {
        Header header = record.headers().lastHeader(headerKey);
        if (header != null && header.value() != null) {
            return new String(header.value(), StandardCharsets.UTF_8);
        }
        return defaultValue;
    }

    public int getDlqReceivedCount() {
        return dlqReceivedCounter.get();
    }

    public void resetCounters() {
        dlqReceivedCounter.set(0);
    }
}
