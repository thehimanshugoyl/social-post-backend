package com.himanshu.social_post_backend.controller;

import com.himanshu.social_post_backend.common.api.ApiResponse;
import com.himanshu.social_post_backend.dto.event.ScheduledPostEvent;
import com.himanshu.social_post_backend.dto.request.SchedulePostEventRequest;
import com.himanshu.social_post_backend.dto.response.ScheduledPostEventResponse;
import com.himanshu.social_post_backend.model.DlqMessage;
import com.himanshu.social_post_backend.model.ProcessedEvent;
import com.himanshu.social_post_backend.service.DlqManagementService;
import com.himanshu.social_post_backend.service.IdempotencyService;
import com.himanshu.social_post_backend.service.ScheduledPostProducerService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller exposing endpoints to demonstrate Experiment 3.1.1 reliability mechanisms:
 * - Asynchronous message publishing via Kafka producer
 * - Idempotency testing and deduplication verification
 * - Transient failure retry simulation with exponential backoff
 * - Dead Letter Queue (DLQ) inspection and message recovery
 */
@RestController
@RequestMapping("/api/v1/events")
public class KafkaEventController {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventController.class);

    private final ScheduledPostProducerService producerService;
    private final IdempotencyService idempotencyService;
    private final DlqManagementService dlqManagementService;
    private final com.himanshu.social_post_backend.consumer.ScheduledPostConsumer scheduledPostConsumer;
    private final com.himanshu.social_post_backend.consumer.ScheduledPostDlqConsumer scheduledPostDlqConsumer;

    @Value("${app.kafka.topics.scheduled-posts:social-scheduled-posts}")
    private String scheduledPostsTopic;

    public KafkaEventController(ScheduledPostProducerService producerService,
                                IdempotencyService idempotencyService,
                                DlqManagementService dlqManagementService,
                                com.himanshu.social_post_backend.consumer.ScheduledPostConsumer scheduledPostConsumer,
                                com.himanshu.social_post_backend.consumer.ScheduledPostDlqConsumer scheduledPostDlqConsumer) {
        this.producerService = producerService;
        this.idempotencyService = idempotencyService;
        this.dlqManagementService = dlqManagementService;
        this.scheduledPostConsumer = scheduledPostConsumer;
        this.scheduledPostDlqConsumer = scheduledPostDlqConsumer;
    }

    /**
     * Publishes a scheduled post event asynchronously to Kafka.
     */
    @PostMapping("/scheduled-posts")
    public ResponseEntity<ApiResponse<ScheduledPostEventResponse>> schedulePost(
            @Valid @RequestBody SchedulePostEventRequest request) {

        ScheduledPostEvent event = new ScheduledPostEvent();
        if (request.getCustomEventId() != null && !request.getCustomEventId().isBlank()) {
            event.setEventId(request.getCustomEventId());
        }
        event.setPostId(request.getPostId());
        event.setTitle(request.getTitle() != null ? request.getTitle() : "Scheduled Post " + UUID.randomUUID().toString().substring(0, 8));
        event.setSlug(request.getSlug() != null ? request.getSlug() : "scheduled-" + UUID.randomUUID().toString().substring(0, 8));
        event.setContent(request.getContent() != null ? request.getContent() : "Automated scheduled post content");
        event.setAuthor(request.getAuthor() != null ? request.getAuthor() : "himanshu");
        event.setTags(request.getTags());
        event.setFailureMode(request.getFailureMode() != null ? request.getFailureMode() : "NONE");
        if (request.getScheduledAt() != null) {
            event.setScheduledAt(request.getScheduledAt());
        }
        if (request.getChannel() != null) {
            event.setChannel(request.getChannel());
        }

        producerService.publishScheduledPost(event);

        ScheduledPostEventResponse response = new ScheduledPostEventResponse(
                event.getEventId(),
                "QUEUED",
                scheduledPostsTopic,
                event.getFailureMode(),
                "Scheduled post event dispatched to Kafka for asynchronous execution."
        );

        ApiResponse<ScheduledPostEventResponse> apiResponse = new ApiResponse<>(
                true,
                HttpStatus.ACCEPTED.value(),
                "Event queued successfully",
                response,
                Instant.now()
        );

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(apiResponse);
    }

    /**
     * Demonstrates idempotency by publishing two messages with the EXACT same event ID.
     */
    @PostMapping("/scheduled-posts/idempotent-test")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testIdempotency() {
        String sharedEventId = "idem-test-" + UUID.randomUUID();
        String slug = "idempotent-slug-" + sharedEventId.substring(10, 18);

        ScheduledPostEvent event1 = new ScheduledPostEvent();
        event1.setEventId(sharedEventId);
        event1.setTitle("Idempotent Test Post");
        event1.setSlug(slug);
        event1.setContent("Testing idempotency and deduplication.");
        event1.setAuthor("himanshu");

        ScheduledPostEvent event2 = new ScheduledPostEvent();
        event2.setEventId(sharedEventId); // Identical event ID
        event2.setTitle("Duplicate Event Post");
        event2.setSlug(slug);
        event2.setContent("Duplicate payload should be skipped.");
        event2.setAuthor("himanshu");

        // Send both messages to Kafka
        producerService.publishScheduledPost(event1);
        producerService.publishScheduledPost(event2);

        Map<String, Object> result = new HashMap<>();
        result.put("sharedEventId", sharedEventId);
        result.put("messagesSent", 2);
        result.put("expectedProcessing", "First message processes successfully; second message skipped due to idempotency.");

        return ResponseEntity.ok(ApiResponse.ok(result, "Idempotency test events dispatched"));
    }

    /**
     * Demonstrates failure handling:
     * - TRANSIENT: retried 3 times with exponential backoff, then sent to DLQ.
     * - FATAL: routed immediately to DLQ without retries.
     */
    @PostMapping("/scheduled-posts/simulate-failure")
    public ResponseEntity<ApiResponse<ScheduledPostEventResponse>> simulateFailure(
            @RequestParam(defaultValue = "TRANSIENT") String mode) {

        ScheduledPostEvent event = new ScheduledPostEvent();
        event.setTitle("Simulated Failure Test (" + mode + ")");
        event.setSlug("sim-failure-" + UUID.randomUUID().toString().substring(0, 8));
        event.setContent("Failure simulation event for testing backoff and DLQ routing.");
        event.setAuthor("tester");
        event.setFailureMode(mode.toUpperCase());

        producerService.publishScheduledPost(event);

        ScheduledPostEventResponse response = new ScheduledPostEventResponse(
                event.getEventId(),
                "QUEUED_FOR_FAILURE_TEST",
                scheduledPostsTopic,
                event.getFailureMode(),
                "Failure simulation event queued. Mode: " + mode.toUpperCase()
        );

        ApiResponse<ScheduledPostEventResponse> apiResponse = new ApiResponse<>(
                true,
                HttpStatus.ACCEPTED.value(),
                "Failure simulation event queued",
                response,
                Instant.now()
        );

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(apiResponse);
    }

    /**
     * Inspects all DLQ messages for auditing failed events.
     */
    @GetMapping("/dlq")
    public ResponseEntity<ApiResponse<List<DlqMessage>>> getAllDlqMessages() {
        List<DlqMessage> dlqMessages = dlqManagementService.getAllDlqMessages();
        return ResponseEntity.ok(ApiResponse.ok(dlqMessages, "Retrieved DLQ messages successfully"));
    }

    /**
     * Inspects a specific DLQ message by ID.
     */
    @GetMapping("/dlq/{id}")
    public ResponseEntity<ApiResponse<DlqMessage>> getDlqMessageById(@PathVariable Long id) {
        return dlqManagementService.getDlqMessageById(id)
                .map(msg -> ResponseEntity.ok(ApiResponse.ok(msg, "Retrieved DLQ message")))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    /**
     * Replays a dead-lettered message from the DLQ back to the main queue for recovery.
     */
    @PostMapping("/dlq/{id}/replay")
    public ResponseEntity<ApiResponse<String>> replayDlqMessage(@PathVariable Long id) {
        dlqManagementService.replayDlqMessage(id);
        return ResponseEntity.ok(ApiResponse.ok("Replayed ID: " + id, "DLQ message replayed successfully to original topic"));
    }

    /**
     * Checks the idempotency tracking record for an event ID.
     */
    @GetMapping("/idempotency/{eventId}")
    public ResponseEntity<ApiResponse<ProcessedEvent>> getIdempotencyStatus(@PathVariable String eventId) {
        return idempotencyService.getEvent(eventId)
                .map(event -> ResponseEntity.ok(ApiResponse.ok(event, "Found idempotency event record")))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    /**
     * Retrieves comprehensive system reliability and fault-tolerance metrics (Exp 3.1.2 - CO5/CO6).
     */
    @GetMapping("/metrics")
    public ResponseEntity<ApiResponse<com.himanshu.social_post_backend.dto.response.KafkaReliabilityMetricsResponse>> getReliabilityMetrics() {
        long processed = scheduledPostConsumer.getProcessedCount();
        long duplicates = scheduledPostConsumer.getDuplicateSkipCount();
        long retries = scheduledPostConsumer.getRetryAttemptCount();
        List<DlqMessage> allDlq = dlqManagementService.getAllDlqMessages();
        long dlqCount = allDlq.size();
        long dlqResolved = allDlq.stream().filter(DlqMessage::isResolved).count();
        long dlqUnresolved = dlqCount - dlqResolved;
        String health = (dlqUnresolved > 10) ? "DEGRADED" : "OPTIMAL";

        com.himanshu.social_post_backend.dto.response.KafkaReliabilityMetricsResponse metrics =
                new com.himanshu.social_post_backend.dto.response.KafkaReliabilityMetricsResponse(
                        processed, duplicates, retries, dlqCount, dlqResolved, dlqUnresolved, health
                );

        return ResponseEntity.ok(ApiResponse.ok(metrics, "Kafka reliability diagnostic metrics retrieved"));
    }

    /**
     * Performs bulk replay of all unresolved dead-letter queue messages for incident remediation.
     */
    @PostMapping("/dlq/replay-all")
    public ResponseEntity<ApiResponse<Map<String, Object>>> replayAllDlqMessages() {
        int replayedCount = dlqManagementService.replayAllUnresolvedMessages();
        Map<String, Object> result = new HashMap<>();
        result.put("replayedCount", replayedCount);
        result.put("status", "SUCCESS");
        result.put("message", "All unresolved DLQ events re-queued for execution.");
        return ResponseEntity.ok(ApiResponse.ok(result, "Bulk DLQ replay triggered successfully"));
    }
}
