package com.himanshu.social_post_backend.dto.response;

import java.time.Instant;

public class ScheduledPostEventResponse {

    private String eventId;
    private String status;
    private String topic;
    private String failureMode;
    private Instant queuedAt;
    private String message;

    public ScheduledPostEventResponse() {
    }

    public ScheduledPostEventResponse(String eventId, String status, String topic, String failureMode, String message) {
        this.eventId = eventId;
        this.status = status;
        this.topic = topic;
        this.failureMode = failureMode;
        this.queuedAt = Instant.now();
        this.message = message;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getFailureMode() {
        return failureMode;
    }

    public void setFailureMode(String failureMode) {
        this.failureMode = failureMode;
    }

    public Instant getQueuedAt() {
        return queuedAt;
    }

    public void setQueuedAt(Instant queuedAt) {
        this.queuedAt = queuedAt;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
