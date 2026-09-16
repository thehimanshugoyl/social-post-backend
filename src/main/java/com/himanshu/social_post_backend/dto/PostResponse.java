package com.himanshu.social_post_backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public class PostResponse {

    private Long id;
    private String text;
    private List<String> platformIds;
    private String status;
    private LocalDateTime scheduledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public PostResponse(Long id, String text, List<String> platformIds, String status,
                         LocalDateTime scheduledAt, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.text = text;
        this.platformIds = platformIds;
        this.status = status;
        this.scheduledAt = scheduledAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public String getText() { return text; }
    public List<String> getPlatformIds() { return platformIds; }
    public String getStatus() { return status; }
    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}