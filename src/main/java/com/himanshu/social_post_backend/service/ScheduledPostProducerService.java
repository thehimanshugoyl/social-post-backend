package com.himanshu.social_post_backend.service;

import com.himanshu.social_post_backend.dto.event.ScheduledPostEvent;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

public interface ScheduledPostProducerService {

    CompletableFuture<SendResult<String, String>> publishScheduledPost(ScheduledPostEvent event);

    CompletableFuture<SendResult<String, String>> publishToTopic(String topic, ScheduledPostEvent event);
}
