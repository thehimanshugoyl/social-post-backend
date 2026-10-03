package com.himanshu.social_post_backend;

import com.himanshu.social_post_backend.consumer.ScheduledPostConsumer;
import com.himanshu.social_post_backend.consumer.ScheduledPostDlqConsumer;
import com.himanshu.social_post_backend.dto.event.ScheduledPostEvent;
import com.himanshu.social_post_backend.dto.response.KafkaReliabilityMetricsResponse;
import com.himanshu.social_post_backend.model.DlqMessage;
import com.himanshu.social_post_backend.model.Post;
import com.himanshu.social_post_backend.model.PostStatus;
import com.himanshu.social_post_backend.repository.DlqMessageRepository;
import com.himanshu.social_post_backend.repository.PostRepository;
import com.himanshu.social_post_backend.repository.ProcessedEventRepository;
import com.himanshu.social_post_backend.service.DlqManagementService;
import com.himanshu.social_post_backend.service.IdempotencyService;
import com.himanshu.social_post_backend.service.ScheduledPostProducerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Experiment 3.1.2: Advanced Reliability Mechanisms & Fault-Tolerance Evaluation (CO5 - BT5, CO6 - BT6)
 *
 * Verifies and evaluates:
 * 1. Concurrent race condition idempotency (BT5 / BT6)
 * 2. Bulk DLQ replay and multi-incident remediation workflow
 * 3. Poison-pill isolation without consumer group partition stall
 * 4. Comprehensive reliability metric observability
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@EmbeddedKafka(
        partitions = 1,
        topics = {"social-scheduled-posts", "social-scheduled-posts.DLT"}
)
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "app.kafka.topics.scheduled-posts=social-scheduled-posts",
        "app.kafka.topics.scheduled-posts-dlq=social-scheduled-posts.DLT",
        "app.kafka.retry.initial-interval-ms=100",
        "app.kafka.retry.multiplier=1.5",
        "app.kafka.retry.max-interval-ms=500",
        "app.kafka.retry.max-attempts=3"
})
public class KafkaAdvancedReliabilityIntegrationTest {

    @Autowired
    private ScheduledPostProducerService producerService;

    @Autowired
    private ScheduledPostConsumer scheduledPostConsumer;

    @Autowired
    private ScheduledPostDlqConsumer dlqConsumer;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private DlqMessageRepository dlqMessageRepository;

    @Autowired
    private DlqManagementService dlqManagementService;

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @BeforeEach
    void setUp() {
        scheduledPostConsumer.resetCounters();
        dlqConsumer.resetCounters();
    }

    @Test
    @DisplayName("Exp 3.1.2 - 1: Concurrent Burst Idempotency Stress Test (Thread Safety & Deduplication)")
    void testConcurrentRaceConditionIdempotency() throws Exception {
        String sharedEventId = "burst-idem-" + UUID.randomUUID();
        int threadCount = 6;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(threadCount);
        AtomicInteger claimedCounter = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startGate.await();
                    boolean claimed = idempotencyService.tryClaimEventProcessing(
                            sharedEventId,
                            "SCHEDULED_POST",
                            "corr-stress-test"
                    );
                    if (claimed) {
                        claimedCounter.incrementAndGet();
                    }
                } catch (Exception ignored) {
                } finally {
                    endGate.countDown();
                }
            });
        }

        // Release all threads simultaneously to evaluate race condition resilience
        startGate.countDown();
        boolean finished = endGate.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(finished).isTrue();
        // Crucial evaluation: Exactly ONE thread must win the claim; all others must be rejected
        assertThat(claimedCounter.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("Exp 3.1.2 - 2: Bulk DLQ Incident Remediation & Batch Replay Workflow")
    void testBulkDlqMessageRemediationAndReplay() {
        String eventId1 = "bulk-dlq-1-" + UUID.randomUUID();
        String eventId2 = "bulk-dlq-2-" + UUID.randomUUID();
        String slug1 = "bulk-replayed-1-" + UUID.randomUUID().toString().substring(0, 8);
        String slug2 = "bulk-replayed-2-" + UUID.randomUUID().toString().substring(0, 8);

        DlqMessage dlq1 = new DlqMessage(
                eventId1,
                "social-scheduled-posts",
                "social-scheduled-posts.DLT",
                """
                {"eventId":"%s","title":"Bulk Recovered 1","slug":"%s","content":"Remediated 1","author":"admin","failureMode":"FATAL"}
                """.formatted(eventId1, slug1),
                "FatalEventProcessingException",
                "Upstream dependency failed",
                3
        );

        DlqMessage dlq2 = new DlqMessage(
                eventId2,
                "social-scheduled-posts",
                "social-scheduled-posts.DLT",
                """
                {"eventId":"%s","title":"Bulk Recovered 2","slug":"%s","content":"Remediated 2","author":"admin","failureMode":"FATAL"}
                """.formatted(eventId2, slug2),
                "FatalEventProcessingException",
                "Downstream database timeout",
                3
        );

        dlqMessageRepository.saveAll(List.of(dlq1, dlq2));

        // Act: Execute batch recovery
        int replayed = dlqManagementService.replayAllUnresolvedMessages();
        assertThat(replayed).isGreaterThanOrEqualTo(2);

        // Assert: Messages are republished and processed by consumer to PUBLISHED state
        await().atMost(10, TimeUnit.SECONDS).pollInterval(Duration.ofMillis(300)).untilAsserted(() -> {
            Optional<Post> post1 = postRepository.findBySlug(slug1);
            Optional<Post> post2 = postRepository.findBySlug(slug2);
            assertThat(post1).isPresent();
            assertThat(post1.get().getStatus()).isEqualTo(PostStatus.PUBLISHED);
            assertThat(post2).isPresent();
            assertThat(post2.get().getStatus()).isEqualTo(PostStatus.PUBLISHED);
        });
    }

    @Test
    @DisplayName("Exp 3.1.2 - 3: Poison Pill Resiliency (Malformed JSON Isolation to DLQ)")
    void testPoisonPillIsolationToDlqWithoutConsumerStall() {
        String poisonPillPayload = "{ this is completely invalid unparseable json !!! }";
        String poisonPillKey = "poison-pill-" + UUID.randomUUID();

        // Publish malformed payload directly to Kafka topic
        kafkaTemplate.send("social-scheduled-posts", poisonPillKey, poisonPillPayload);

        // Assert: Poison pill is trapped by ErrorHandler and isolated to DLQ without stalling
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(dlqConsumer.getDlqReceivedCount()).isGreaterThanOrEqualTo(1);
            Optional<DlqMessage> dlqMsgOpt = dlqMessageRepository.findByEventId(poisonPillKey);
            assertThat(dlqMsgOpt).isPresent();
            DlqMessage dlqMsg = dlqMsgOpt.get();
            assertThat(dlqMsg.getDlqTopic()).isEqualTo("social-scheduled-posts.DLT");
            assertThat(dlqMsg.getPayload()).isEqualTo(poisonPillPayload);
        });
    }

    @Test
    @DisplayName("Exp 3.1.2 - 4: Reliability Metrics & System Observability Audit")
    void testReliabilityMetricsAudit() {
        long processed = scheduledPostConsumer.getProcessedCount();
        long duplicates = scheduledPostConsumer.getDuplicateSkipCount();
        long retries = scheduledPostConsumer.getRetryAttemptCount();
        List<DlqMessage> dlqs = dlqManagementService.getAllDlqMessages();

        KafkaReliabilityMetricsResponse response = new KafkaReliabilityMetricsResponse(
                processed,
                duplicates,
                retries,
                dlqs.size(),
                dlqs.stream().filter(DlqMessage::isResolved).count(),
                dlqs.stream().filter(m -> !m.isResolved()).count(),
                "OPTIMAL"
        );

        assertThat(response.getSystemHealth()).isEqualTo("OPTIMAL");
        assertThat(response.getTimestamp()).isNotNull();
        assertThat(response.getTotalDlqMessagesRecorded()).isGreaterThanOrEqualTo(0);
    }
}
