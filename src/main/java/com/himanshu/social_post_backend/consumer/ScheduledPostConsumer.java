package com.himanshu.social_post_backend.consumer;

import tools.jackson.databind.ObjectMapper;
import com.himanshu.social_post_backend.common.exception.FatalEventProcessingException;
import com.himanshu.social_post_backend.common.exception.TransientEventProcessingException;
import com.himanshu.social_post_backend.dto.event.ScheduledPostEvent;
import com.himanshu.social_post_backend.model.Post;
import com.himanshu.social_post_backend.model.PostStatus;
import com.himanshu.social_post_backend.repository.PostRepository;
import com.himanshu.social_post_backend.service.IdempotencyService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Kafka Consumer for processing scheduled posts asynchronously.
 * Demonstrates:
 * 1. Idempotency verification: Skips duplicate messages using ProcessedEvent tracking.
 * 2. Exponential backoff retry: Triggers retry attempts on TransientEventProcessingException.
 * 3. Dead Letter Queue routing: Routes unrecoverable failures to DLQ upon retry exhaustion.
 */
@Component
public class ScheduledPostConsumer {

    private static final Logger log = LoggerFactory.getLogger(ScheduledPostConsumer.class);

    private final PostRepository postRepository;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    // For testing and metrics assertion: counts executions
    private final AtomicInteger processedCounter = new AtomicInteger(0);
    private final AtomicInteger duplicateSkipCounter = new AtomicInteger(0);
    private final AtomicInteger retryAttemptCounter = new AtomicInteger(0);

    public ScheduledPostConsumer(PostRepository postRepository,
                                 IdempotencyService idempotencyService,
                                 ObjectMapper objectMapper) {
        this.postRepository = postRepository;
        this.idempotencyService = idempotencyService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.scheduled-posts:social-scheduled-posts}",
            groupId = "${spring.kafka.consumer.group-id:social-post-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void consumeScheduledPost(ConsumerRecord<String, String> record) {
        String payload = record.value();
        ScheduledPostEvent event;
        try {
            event = objectMapper.readValue(payload, ScheduledPostEvent.class);
        } catch (Exception e) {
            log.error("[Kafka-Consumer] Fatal deserialization error for record at offset {}: {}", record.offset(), e.getMessage());
            throw new FatalEventProcessingException("Malformed JSON payload in topic " + record.topic(), e);
        }

        String correlationId = event.getCorrelationId();
        if (correlationId != null) {
            MDC.put("correlationId", correlationId);
        }

        try {
            log.info("[Kafka-Consumer] Received scheduled post event. eventId='{}', slug='{}', failureMode='{}', topic='{}', partition={}, offset={}",
                    event.getEventId(), event.getSlug(), event.getFailureMode(), record.topic(), record.partition(), record.offset());

            // 1. Idempotency Check: Guard against duplicate processing
            if (idempotencyService.isEventProcessed(event.getEventId())) {
                duplicateSkipCounter.incrementAndGet();
                idempotencyService.markEventDuplicateSkipped(event.getEventId());
                log.warn("[Idempotency] SKIPPING DUPLICATE: Event '{}' has already been successfully processed. No duplicate side effects executed.",
                        event.getEventId());
                return;
            }

            // 2. Failure Simulation for Testing Reliability Mechanisms
            if ("TRANSIENT".equalsIgnoreCase(event.getFailureMode())) {
                retryAttemptCounter.incrementAndGet();
                log.warn("[Failure-Simulation] Throwing TransientEventProcessingException for eventId='{}'. Attempt #{}",
                        event.getEventId(), retryAttemptCounter.get());
                throw new TransientEventProcessingException("Simulated transient network timeout for eventId: " + event.getEventId());
            }

            if ("FATAL".equalsIgnoreCase(event.getFailureMode())) {
                log.error("[Failure-Simulation] Throwing FatalEventProcessingException for eventId='{}'. Routing immediately to DLQ.",
                        event.getEventId());
                throw new FatalEventProcessingException("Simulated fatal unrecoverable payload error for eventId: " + event.getEventId());
            }

            // 3. Business Processing: Publish or Create Post
            Post post;
            if (event.getPostId() != null) {
                post = postRepository.findById(event.getPostId()).orElseGet(() -> createNewPostFromEvent(event));
            } else {
                post = postRepository.findBySlug(event.getSlug()).orElseGet(() -> createNewPostFromEvent(event));
            }

            post.setStatus(PostStatus.PUBLISHED);
            Post savedPost = postRepository.save(post);

            // 4. Mark Idempotency as PROCESSED
            idempotencyService.markEventSuccess(
                    event.getEventId(),
                    savedPost.getId().toString(),
                    "Successfully published post '" + savedPost.getTitle() + "' with ID: " + savedPost.getId()
            );

            processedCounter.incrementAndGet();
            log.info("[Kafka-Consumer] Successfully processed scheduled post id={}, slug='{}', status={}",
                    savedPost.getId(), savedPost.getSlug(), savedPost.getStatus());

        } finally {
            MDC.remove("correlationId");
        }
    }

    private Post createNewPostFromEvent(ScheduledPostEvent event) {
        Post post = new Post();
        post.setTitle(event.getTitle() != null ? event.getTitle() : "Scheduled Post " + event.getEventId().substring(0, 8));
        post.setSlug(event.getSlug() != null ? event.getSlug() : "scheduled-" + event.getEventId().substring(0, 8));
        post.setContent(event.getContent() != null ? event.getContent() : "Content scheduled for publishing.");
        post.setAuthor(event.getAuthor() != null ? event.getAuthor() : "system");
        post.setTags(event.getTags());
        post.setStatus(PostStatus.PUBLISHED);
        return post;
    }

    public int getProcessedCount() {
        return processedCounter.get();
    }

    public int getDuplicateSkipCount() {
        return duplicateSkipCounter.get();
    }

    public int getRetryAttemptCount() {
        return retryAttemptCounter.get();
    }

    public void resetCounters() {
        processedCounter.set(0);
        duplicateSkipCounter.set(0);
        retryAttemptCounter.set(0);
    }
}
