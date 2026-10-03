package com.himanshu.social_post_backend.config;

import com.himanshu.social_post_backend.common.exception.FatalEventProcessingException;
import com.himanshu.social_post_backend.common.exception.TransientEventProcessingException;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Apache Kafka Configuration for Experiment 3.1.1.
 * Configures:
 * 1. Topics: Primary scheduled posts topic and dedicated Dead Letter Queue (DLQ) topic.
 * 2. Producer: Idempotent producer with acks=all and retries.
 * 3. Consumer: Consumer factory with String serialization.
 * 4. Reliability Mechanisms:
 *    - Retry mechanism with Exponential Backoff (initial interval, multiplier, max attempts).
 *    - DeadLetterPublishingRecoverer for routing exhausted retries and fatal errors directly to DLQ.
 */
@Configuration
@EnableKafka
public class KafkaConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConfig.class);

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:social-post-group}")
    private String groupId;

    @Value("${app.kafka.topics.scheduled-posts:social-scheduled-posts}")
    private String scheduledPostsTopic;

    @Value("${app.kafka.topics.scheduled-posts-dlq:social-scheduled-posts.DLT}")
    private String scheduledPostsDlqTopic;

    @Value("${app.kafka.retry.initial-interval-ms:500}")
    private long retryInitialIntervalMs;

    @Value("${app.kafka.retry.multiplier:2.0}")
    private double retryMultiplier;

    @Value("${app.kafka.retry.max-interval-ms:5000}")
    private long retryMaxIntervalMs;

    @Value("${app.kafka.retry.max-attempts:3}")
    private int retryMaxAttempts;

    // --- Topic Beans ---

    @Bean
    public NewTopic scheduledPostsTopic() {
        return TopicBuilder.name(scheduledPostsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic scheduledPostsDlqTopic() {
        return TopicBuilder.name(scheduledPostsDlqTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    // --- Producer Configuration ---

    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        // Producer-side idempotency guarantees exactly-once delivery per producer session
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // --- Consumer Configuration ---

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new DefaultKafkaConsumerFactory<>(props);
    }

    // --- Reliability: Retry with Exponential Backoff & DLQ Recoverer ---

    @Bean
    public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(KafkaTemplate<String, String> kafkaTemplate) {
        return new DeadLetterPublishingRecoverer(kafkaTemplate, (record, exception) -> {
            log.error("[DLQ-Router] Routing failed message to DLQ topic '{}' for record key '{}' after retries. Reason: {}",
                    scheduledPostsDlqTopic, record.key(), exception.getMessage());
            return new TopicPartition(scheduledPostsDlqTopic, record.partition() >= 0 ? record.partition() : 0);
        });
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(DeadLetterPublishingRecoverer deadLetterPublishingRecoverer) {
        ExponentialBackOff backOff = new ExponentialBackOff(retryInitialIntervalMs, retryMultiplier);
        backOff.setMaxInterval(retryMaxIntervalMs);
        backOff.setMaxElapsedTime(retryInitialIntervalMs * retryMaxAttempts * 3);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(deadLetterPublishingRecoverer, backOff);

        // Retry transient errors with exponential backoff
        errorHandler.addRetryableExceptions(TransientEventProcessingException.class);

        // Do not waste retries on fatal/unrecoverable errors; route immediately to DLQ
        errorHandler.addNotRetryableExceptions(FatalEventProcessingException.class);

        errorHandler.setRetryListeners((record, ex, deliveryAttempt) -> {
            log.warn("[Kafka-Retry] Attempt #{} failed for topic '{}', partition '{}', offset '{}'. Cause: {}. Backing off...",
                    deliveryAttempt, record.topic(), record.partition(), record.offset(), ex.getMessage());
        });

        return errorHandler;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory,
            DefaultErrorHandler kafkaErrorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(kafkaErrorHandler);
        factory.setConcurrency(3);
        return factory;
    }
}
