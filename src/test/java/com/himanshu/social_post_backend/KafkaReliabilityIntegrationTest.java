package com.himanshu.social_post_backend;

import com.himanshu.social_post_backend.consumer.ScheduledPostConsumer;
import com.himanshu.social_post_backend.consumer.ScheduledPostDlqConsumer;
import com.himanshu.social_post_backend.dto.event.ScheduledPostEvent;
import com.himanshu.social_post_backend.model.DlqMessage;
import com.himanshu.social_post_backend.model.Post;
import com.himanshu.social_post_backend.model.PostStatus;
import com.himanshu.social_post_backend.model.ProcessedEvent;
import com.himanshu.social_post_backend.model.ProcessedEventStatus;
import com.himanshu.social_post_backend.repository.DlqMessageRepository;
import com.himanshu.social_post_backend.repository.PostRepository;
import com.himanshu.social_post_backend.repository.ProcessedEventRepository;
import com.himanshu.social_post_backend.service.DlqManagementService;
import com.himanshu.social_post_backend.service.ScheduledPostProducerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Experiment 3.1.1: Event-Driven Communication System using Apache Kafka
 * Comprehensive integration test suite verifying:
 * 1. Asynchronous processing of scheduled posts (Producer & Consumer)
 * 2. Idempotency pattern and duplicate message suppression
 * 3. Exponential backoff retry mechanism upon transient failure
 * 4. Dead Letter Queue (DLQ) routing upon retry exhaustion
 * 5. Immediate DLQ routing for fatal non-retryable errors
 * 6. DLQ message replay recovery workflow
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
public class KafkaReliabilityIntegrationTest {

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

    @BeforeEach
    void setUp() {
        scheduledPostConsumer.resetCounters();
        dlqConsumer.resetCounters();
    }

    @Test
    @DisplayName("Exp 3.1.1 - 1: Asynchronous Scheduled Post Processing (Producer -> Consumer)")
    void testAsynchronousScheduledPostProcessing() {
        // Arrange
        String eventId = "evt-async-" + UUID.randomUUID();
        String slug = "async-slug-" + UUID.randomUUID().toString().substring(0, 8);

        ScheduledPostEvent event = new ScheduledPostEvent();
        event.setEventId(eventId);
        event.setTitle("Kafka Asynchronous Scheduled Post");
        event.setSlug(slug);
        event.setContent("Content delivered asynchronously via Apache Kafka event bus.");
        event.setAuthor("himanshu-goyal");
        event.setTags(Set.of("kafka", "async", "exp3.1.1"));
        event.setFailureMode("NONE");

        // Act
        producerService.publishScheduledPost(event);

        // Assert: Asynchronously wait until post is consumed and published
        await().atMost(10, TimeUnit.SECONDS).pollInterval(Duration.ofMillis(200)).untilAsserted(() -> {
            Optional<Post> postOpt = postRepository.findBySlug(slug);
            assertThat(postOpt).isPresent();
            Post post = postOpt.get();
            assertThat(post.getStatus()).isEqualTo(PostStatus.PUBLISHED);
            assertThat(post.getTitle()).isEqualTo("Kafka Asynchronous Scheduled Post");

            // Verify Idempotency record
            Optional<ProcessedEvent> eventOpt = processedEventRepository.findByEventId(eventId);
            assertThat(eventOpt).isPresent();
            assertThat(eventOpt.get().getStatus()).isEqualTo(ProcessedEventStatus.PROCESSED);
        });
    }

    @Test
    @DisplayName("Exp 3.1.1 - 2: Idempotency Pattern - Deduplicates re-delivered messages")
    void testIdempotentEventProcessingSuppressesDuplicates() {
        // Arrange
        String sharedEventId = "idem-key-" + UUID.randomUUID();
        String slug = "idempotency-test-" + UUID.randomUUID().toString().substring(0, 8);

        ScheduledPostEvent event1 = new ScheduledPostEvent();
        event1.setEventId(sharedEventId);
        event1.setTitle("Original Scheduled Post");
        event1.setSlug(slug);
        event1.setContent("Original message body");
        event1.setAuthor("himanshu-goyal");
        event1.setFailureMode("NONE");

        ScheduledPostEvent event2 = new ScheduledPostEvent();
        event2.setEventId(sharedEventId); // IDENTICAL eventId to simulate Kafka duplicate delivery
        event2.setTitle("Duplicate Redelivered Post");
        event2.setSlug(slug);
        event2.setContent("Duplicate payload body");
        event2.setAuthor("himanshu-goyal");
        event2.setFailureMode("NONE");

        // Act: Publish both messages
        producerService.publishScheduledPost(event1);

        // Wait for first message to finish
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(postRepository.findBySlug(slug)).isPresent();
        });

        int initialProcessedCount = scheduledPostConsumer.getProcessedCount();

        // Publish duplicate message
        producerService.publishScheduledPost(event2);

        // Assert: Wait and verify duplicate message was skipped without second DB write
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(scheduledPostConsumer.getDuplicateSkipCount()).isGreaterThanOrEqualTo(1);
            // Processed count must not have increased
            assertThat(scheduledPostConsumer.getProcessedCount()).isEqualTo(initialProcessedCount);

            // ProcessedEvent should record duplicate skip
            Optional<ProcessedEvent> recordOpt = processedEventRepository.findByEventId(sharedEventId);
            assertThat(recordOpt).isPresent();
            assertThat(recordOpt.get().getAttemptsCount()).isGreaterThanOrEqualTo(2);
        });
    }

    @Test
    @DisplayName("Exp 3.1.1 - 3: Exponential Backoff Retries & Routing to Dead Letter Queue (DLQ)")
    void testRetryExhaustionRoutesToDlq() {
        // Arrange
        String eventId = "retry-dlq-" + UUID.randomUUID();
        String slug = "retry-slug-" + UUID.randomUUID().toString().substring(0, 8);

        ScheduledPostEvent event = new ScheduledPostEvent();
        event.setEventId(eventId);
        event.setTitle("Transient Failure Test Post");
        event.setSlug(slug);
        event.setContent("Triggers transient network failure to exercise exponential backoff.");
        event.setAuthor("tester");
        event.setFailureMode("TRANSIENT"); // triggers retryable exception

        // Act
        producerService.publishScheduledPost(event);

        // Assert: Consumer retries multiple times, then routes to DLQ
        await().atMost(15, TimeUnit.SECONDS).pollInterval(Duration.ofMillis(300)).untilAsserted(() -> {
            // Verify retries took place
            assertThat(scheduledPostConsumer.getRetryAttemptCount()).isGreaterThanOrEqualTo(2);

            // Verify message landed in DLQ
            assertThat(dlqConsumer.getDlqReceivedCount()).isGreaterThanOrEqualTo(1);

            // Verify DLQ message persisted in database for audit
            Optional<DlqMessage> dlqMsgOpt = dlqMessageRepository.findByEventId(eventId);
            assertThat(dlqMsgOpt).isPresent();
            DlqMessage dlqMsg = dlqMsgOpt.get();
            assertThat(dlqMsg.getOriginalTopic()).isEqualTo("social-scheduled-posts");
            assertThat(dlqMsg.getDlqTopic()).isEqualTo("social-scheduled-posts.DLT");
            assertThat(dlqMsg.getExceptionMessage()).contains("Simulated transient network timeout");
            assertThat(dlqMsg.isResolved()).isFalse();

            // Verify Idempotency status marked as FAILED_DLQ
            Optional<ProcessedEvent> eventOpt = processedEventRepository.findByEventId(eventId);
            assertThat(eventOpt).isPresent();
            assertThat(eventOpt.get().getStatus()).isEqualTo(ProcessedEventStatus.FAILED_DLQ);
        });
    }

    @Test
    @DisplayName("Exp 3.1.1 - 4: Fatal Error Bypasses Retries and Routes Directly to DLQ")
    void testFatalErrorRoutesDirectlyToDlqWithoutRetries() {
        // Arrange
        String eventId = "fatal-dlq-" + UUID.randomUUID();

        ScheduledPostEvent event = new ScheduledPostEvent();
        event.setEventId(eventId);
        event.setTitle("Fatal Error Test");
        event.setSlug("fatal-slug-" + UUID.randomUUID().toString().substring(0, 8));
        event.setContent("Simulates unrecoverable payload error.");
        event.setAuthor("tester");
        event.setFailureMode("FATAL"); // non-retryable exception

        // Act
        producerService.publishScheduledPost(event);

        // Assert: DLQ receives message immediately without wasting retry attempts
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Optional<DlqMessage> dlqMsgOpt = dlqMessageRepository.findByEventId(eventId);
            assertThat(dlqMsgOpt).isPresent();
            DlqMessage dlqMsg = dlqMsgOpt.get();
            assertThat(dlqMsg.getExceptionClass()).contains("FatalEventProcessingException");
            assertThat(dlqMsg.getExceptionMessage()).contains("Simulated fatal unrecoverable payload error");
        });
    }

    @Test
    @DisplayName("Exp 3.1.1 - 5: DLQ Replay and Message Recovery Workflow")
    void testDlqMessageReplayAndRecovery() {
        // Arrange: Directly persist a DLQ message representing a resolved upstream incident
        String eventId = "replay-evt-" + UUID.randomUUID();
        String slug = "replayed-post-" + UUID.randomUUID().toString().substring(0, 8);

        ScheduledPostEvent event = new ScheduledPostEvent();
        event.setEventId(eventId);
        event.setTitle("Replayed From DLQ");
        event.setSlug(slug);
        event.setContent("Successfully recovered after incident remediation.");
        event.setAuthor("recovery-agent");
        event.setFailureMode("FATAL"); // Initially failed

        DlqMessage dlqMessage = new DlqMessage(
                eventId,
                "social-scheduled-posts",
                "social-scheduled-posts.DLT",
                """
                {"eventId":"%s","title":"Replayed From DLQ","slug":"%s","content":"Successfully recovered after incident remediation.","author":"recovery-agent","failureMode":"FATAL"}
                """.formatted(eventId, slug),
                "FatalEventProcessingException",
                "Simulated error before remediation",
                3
        );
        DlqMessage savedDlq = dlqMessageRepository.save(dlqMessage);

        // Act: Replay the message via DlqManagementService
        dlqManagementService.replayDlqMessage(savedDlq.getId());

        // Assert: Message is reprocessed and post is created/published successfully
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            // DLQ record marked resolved
            DlqMessage updatedDlq = dlqMessageRepository.findById(savedDlq.getId()).orElseThrow();
            assertThat(updatedDlq.isResolved()).isTrue();
            assertThat(updatedDlq.getResolutionNotes()).contains("Successfully replayed");

            // Post successfully created and published
            Optional<Post> postOpt = postRepository.findBySlug(slug);
            assertThat(postOpt).isPresent();
            assertThat(postOpt.get().getStatus()).isEqualTo(PostStatus.PUBLISHED);
        });
    }
}
