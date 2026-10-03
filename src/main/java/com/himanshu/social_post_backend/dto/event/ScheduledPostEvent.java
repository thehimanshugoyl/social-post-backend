package com.himanshu.social_post_backend.dto.event;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Event payload representing a scheduled post for asynchronous processing via Apache Kafka.
 * Contains a unique eventId for idempotency tracking, payload details, and failure simulation flags.
 */
public class ScheduledPostEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String eventId;
    private Long postId;
    private String title;
    private String slug;
    private String content;
    private String author;
    private String channel;
    private Set<String> tags = new HashSet<>();
    private String action;
    private Instant scheduledAt;
    private Instant createdAt;
    private String correlationId;
    private String failureMode; // "NONE", "TRANSIENT", "FATAL"
    private int retryCount;

    public ScheduledPostEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.createdAt = Instant.now();
        this.failureMode = "NONE";
        this.action = "PUBLISH_SCHEDULED_POST";
    }

    public ScheduledPostEvent(Long postId, String title, String slug, String content, String author, Set<String> tags) {
        this();
        this.postId = postId;
        this.title = title;
        this.slug = slug;
        this.content = content;
        this.author = author;
        if (tags != null) {
            this.tags.addAll(tags);
        }
        this.scheduledAt = Instant.now();
    }

    public static ScheduledPostEvent create(Long postId, String title, String slug, String content, String author, Set<String> tags) {
        return new ScheduledPostEvent(postId, title, slug, content, author, tags);
    }

    // Getters and Setters

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
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

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags != null ? tags : new HashSet<>();
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(Instant scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getFailureMode() {
        return failureMode;
    }

    public void setFailureMode(String failureMode) {
        this.failureMode = failureMode;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    @Override
    public String toString() {
        return "ScheduledPostEvent{" +
                "eventId='" + eventId + '\'' +
                ", postId=" + postId +
                ", title='" + title + '\'' +
                ", slug='" + slug + '\'' +
                ", author='" + author + '\'' +
                ", action='" + action + '\'' +
                ", failureMode='" + failureMode + '\'' +
                ", retryCount=" + retryCount +
                '}';
    }
}
