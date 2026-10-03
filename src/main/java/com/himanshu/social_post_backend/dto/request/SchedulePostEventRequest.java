package com.himanshu.social_post_backend.dto.request;

import jakarta.validation.constraints.Size;
import java.util.Set;

public class SchedulePostEventRequest {

    private Long postId;

    @Size(max = 150, message = "Title must not exceed 150 characters")
    private String title;

    @Size(max = 180, message = "Slug must not exceed 180 characters")
    private String slug;

    private String content;

    private String author;

    private Set<String> tags;

    private String failureMode; // "NONE", "TRANSIENT", "FATAL"

    private String customEventId; // For testing idempotency deduplication

    public SchedulePostEventRequest() {
    }

    public SchedulePostEventRequest(String title, String slug, String content, String author, Set<String> tags) {
        this.title = title;
        this.slug = slug;
        this.content = content;
        this.author = author;
        this.tags = tags;
        this.failureMode = "NONE";
    }

    public Long getPostId() {
        return postId;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public String getFailureMode() {
        return failureMode;
    }

    public void setFailureMode(String failureMode) {
        this.failureMode = failureMode;
    }

    public String getCustomEventId() {
        return customEventId;
    }

    public void setCustomEventId(String customEventId) {
        this.customEventId = customEventId;
    }
}
