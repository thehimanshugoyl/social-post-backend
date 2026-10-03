package com.himanshu.social_post_backend.common.exception;

/**
 * Exception indicating an unrecoverable, fatal failure during Kafka event processing
 * (e.g. malformed data payload, business validation failure, unsupported schema).
 * Bypasses retries and routes the message immediately to the Dead Letter Queue (DLQ).
 */
public class FatalEventProcessingException extends RuntimeException {

    public FatalEventProcessingException(String message) {
        super(message);
    }

    public FatalEventProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
