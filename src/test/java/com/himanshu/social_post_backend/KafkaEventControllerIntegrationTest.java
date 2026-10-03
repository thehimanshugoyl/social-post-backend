package com.himanshu.social_post_backend;

import tools.jackson.databind.ObjectMapper;
import com.himanshu.social_post_backend.dto.request.SchedulePostEventRequest;
import com.himanshu.social_post_backend.model.DlqMessage;
import com.himanshu.social_post_backend.model.ProcessedEvent;
import com.himanshu.social_post_backend.model.ProcessedEventStatus;
import com.himanshu.social_post_backend.repository.DlqMessageRepository;
import com.himanshu.social_post_backend.repository.ProcessedEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@EmbeddedKafka(
        partitions = 1,
        topics = {"social-scheduled-posts", "social-scheduled-posts.DLT"}
)
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "app.kafka.retry.initial-interval-ms=100",
        "app.kafka.retry.multiplier=1.5",
        "app.kafka.retry.max-interval-ms=500"
})
class KafkaEventControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DlqMessageRepository dlqMessageRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @DisplayName("POST /api/v1/events/scheduled-posts - Dispatches event and returns 202 Accepted")
    void shouldSchedulePostEventSuccessfully() throws Exception {
        SchedulePostEventRequest request = new SchedulePostEventRequest(
                "Event-Driven Microservices Architecture",
                "event-driven-microservices-slug",
                "Kafka ensures decoupled and asynchronous event handling.",
                "himanshu",
                Set.of("kafka", "eda", "spring")
        );

        mockMvc.perform(post("/api/v1/events/scheduled-posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(202))
                .andExpect(jsonPath("$.data.status").value("QUEUED"))
                .andExpect(jsonPath("$.data.eventId").isNotEmpty())
                .andExpect(jsonPath("$.data.topic").value("social-scheduled-posts"));
    }

    @Test
    @DisplayName("POST /api/v1/events/scheduled-posts/idempotent-test - Triggers idempotency verification")
    void shouldTriggerIdempotentTest() throws Exception {
        mockMvc.perform(post("/api/v1/events/scheduled-posts/idempotent-test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.messagesSent").value(2))
                .andExpect(jsonPath("$.data.sharedEventId").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/v1/events/scheduled-posts/simulate-failure - Simulates retryable failure")
    void shouldSimulateFailureScenario() throws Exception {
        mockMvc.perform(post("/api/v1/events/scheduled-posts/simulate-failure?mode=TRANSIENT"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.failureMode").value("TRANSIENT"))
                .andExpect(jsonPath("$.data.status").value("QUEUED_FOR_FAILURE_TEST"));
    }

    @Test
    @DisplayName("GET /api/v1/events/dlq - Retrieves dead-letter queue records")
    void shouldRetrieveDlqMessages() throws Exception {
        DlqMessage dlq = new DlqMessage(
                "dlq-evt-test-1",
                "social-scheduled-posts",
                "social-scheduled-posts.DLT",
                "{\"title\":\"Failed Post\"}",
                "TransientEventProcessingException",
                "Network timeout during processing",
                3
        );
        dlqMessageRepository.save(dlq);

        mockMvc.perform(get("/api/v1/events/dlq"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("GET /api/v1/events/idempotency/{eventId} - Checks event idempotency status")
    void shouldGetIdempotencyStatus() throws Exception {
        String testEventId = "evt-idem-audit-" + UUID.randomUUID();
        ProcessedEvent event = new ProcessedEvent(
                testEventId,
                "SCHEDULED_POST",
                "42",
                ProcessedEventStatus.PROCESSED,
                "corr-test-123"
        );
        event.setDetails("Processed via unit test");
        processedEventRepository.save(event);

        mockMvc.perform(get("/api/v1/events/idempotency/" + testEventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.eventId").value(testEventId))
                .andExpect(jsonPath("$.data.status").value("PROCESSED"))
                .andExpect(jsonPath("$.data.aggregateId").value("42"));
    }
}
