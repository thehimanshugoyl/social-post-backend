package com.himanshu.social_post_backend.service.impl;

import tools.jackson.databind.ObjectMapper;
import com.himanshu.social_post_backend.common.exception.ResourceNotFoundException;
import com.himanshu.social_post_backend.dto.event.ScheduledPostEvent;
import com.himanshu.social_post_backend.model.DlqMessage;
import com.himanshu.social_post_backend.repository.DlqMessageRepository;
import com.himanshu.social_post_backend.service.DlqManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class DlqManagementServiceImpl implements DlqManagementService {

    private static final Logger log = LoggerFactory.getLogger(DlqManagementServiceImpl.class);

    private final DlqMessageRepository dlqMessageRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public DlqManagementServiceImpl(DlqMessageRepository dlqMessageRepository,
                                    KafkaTemplate<String, String> kafkaTemplate,
                                    ObjectMapper objectMapper) {
        this.dlqMessageRepository = dlqMessageRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public DlqMessage recordDlqMessage(DlqMessage message) {
        DlqMessage saved = dlqMessageRepository.save(message);
        log.warn("[DLQ-Audit] Persisted DLQ message record. id: {}, eventId: '{}', originalTopic: '{}', exception: '{}'",
                saved.getId(), saved.getEventId(), saved.getOriginalTopic(), saved.getExceptionClass());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DlqMessage> getAllDlqMessages() {
        return dlqMessageRepository.findAllByOrderByFailureTimestampDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DlqMessage> getUnresolvedDlqMessages() {
        return dlqMessageRepository.findByResolvedFalse();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DlqMessage> getDlqMessageById(Long id) {
        return dlqMessageRepository.findById(id);
    }

    @Override
    @Transactional
    public DlqMessage markResolved(Long id, String resolutionNotes) {
        DlqMessage message = dlqMessageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DLQ Message", "id", id));
        message.setResolved(true);
        message.setResolutionNotes(resolutionNotes != null ? resolutionNotes : "Manually resolved at " + Instant.now());
        return dlqMessageRepository.save(message);
    }

    @Override
    @Transactional
    public void replayDlqMessage(Long id) {
        DlqMessage dlqMessage = dlqMessageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DLQ Message", "id", id));

        try {
            // Parse event payload, reset failure mode to allow successful reprocessing
            ScheduledPostEvent event = objectMapper.readValue(dlqMessage.getPayload(), ScheduledPostEvent.class);
            event.setFailureMode("NONE");
            String updatedPayload = objectMapper.writeValueAsString(event);

            String targetTopic = dlqMessage.getOriginalTopic();
            kafkaTemplate.send(targetTopic, event.getEventId(), updatedPayload);

            dlqMessage.setResolved(true);
            dlqMessage.setResolutionNotes("Successfully replayed to topic '" + targetTopic + "' at " + Instant.now());
            dlqMessageRepository.save(dlqMessage);

            log.info("[DLQ-Replay] Successfully replayed DLQ event '{}' to topic '{}'", event.getEventId(), targetTopic);
        } catch (Exception e) {
            log.error("[DLQ-Replay] Failed to replay DLQ message with id: {}", id, e);
            throw new RuntimeException("Failed to replay DLQ message: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public int replayAllUnresolvedMessages() {
        List<DlqMessage> unresolved = dlqMessageRepository.findByResolvedFalse();
        int count = 0;
        for (DlqMessage msg : unresolved) {
            try {
                replayDlqMessage(msg.getId());
                count++;
            } catch (Exception e) {
                log.error("[DLQ-BulkReplay] Failed to replay message id: {}", msg.getId(), e);
            }
        }
        log.info("[DLQ-BulkReplay] Completed bulk replay of {} messages.", count);
        return count;
    }
}
