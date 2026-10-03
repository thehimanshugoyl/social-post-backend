package com.himanshu.social_post_backend.model;

public enum ProcessedEventStatus {
    PROCESSED,
    DUPLICATE_SKIPPED,
    FAILED_DLQ
}
