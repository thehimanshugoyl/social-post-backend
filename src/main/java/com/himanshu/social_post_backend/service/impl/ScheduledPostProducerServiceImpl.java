package com.himanshu.social_post_backend.service.impl;

import tools.jackson.databind.ObjectMapper;
import com.himanshu.social_post_backend.dto.event.ScheduledPostEvent;
import com.himanshu.social_post_backend.service.ScheduledPostProducerService;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class ScheduledPostProducerServiceImpl implements ScheduledPostProducerService {

    private static final Logger log = LoggerFactory.getLogger(ScheduledPostProducerServiceImpl.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.kafka.topics.scheduled-posts:social-scheduled-posts}")
    private String scheduledPostsTopic;

    public ScheduledPostProducerServiceImpl(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public CompletableFuture<SendResult<String, String>> publishScheduledPost(ScheduledPostEvent event) {
        return publishToTopic(scheduledPostsTopic, event);
    }

    @Override
    public CompletableFuture<SendResult<String, String>> publishToTopic(String topic, ScheduledPostEvent event) {
        if (event.getEventId() == null || event.getEventId().isBlank()) {
            event.setEventId(UUID.randomUUID().toString());
        }

        String correlationId = event.getCorrelationId();
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = MDC.get("correlationId");
            if (correlationId == null || correlationId.isBlank()) {
                correlationId = UUID.randomUUID().toString();
            }
            event.setCorrelationId(correlationId);
        }

        try {
            String payload = objectMapper.writeValueAsString(event);
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, event.getEventId(), payload);

            // Add standard metadata headers
            record.headers().add(new RecordHeader("X-Correlation-Id", correlationId.getBytes(StandardCharsets.UTF_8)));
            record.headers().add(new RecordHeader("X-Event-Id", event.getEventId().getBytes(StandardCharsets.UTF_8)));
            record.headers().add(new RecordHeader("X-Event-Type", "SCHEDULED_POST".getBytes(StandardCharsets.UTF_8)));

            log.info("[Kafka-Producer] Publishing scheduled post event to topic '{}'. eventId='{}', slug='{}', mode='{}'",
                    topic, event.getEventId(), event.getSlug(), event.getFailureMode());

            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(record);

            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("[Kafka-Producer] Failed to publish eventId='{}' to topic '{}'. Error: {}",
                            event.getEventId(), topic, ex.getMessage(), ex);
                } else {
                    log.info("[Kafka-Producer] Successfully delivered eventId='{}' to topic='{}', partition={}, offset={}",
                            event.getEventId(),
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                }
            });

            return future;
        } catch (Exception e) {
            log.error("[Kafka-Producer] Serialization failed for eventId='{}': {}", event.getEventId(), e.getMessage(), e);
            throw new RuntimeException("Failed to serialize and publish scheduled post event: " + e.getMessage(), e);
        }
    }
}
