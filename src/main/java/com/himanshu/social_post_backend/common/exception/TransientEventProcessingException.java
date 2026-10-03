package com.himanshu.social_post_backend.common.exception;

/**
 * Exception indicating a transient, recoverable failure during Kafka event processing
 * (e.g. temporary network partition, remote service timeout, transient database lock).
 * The consumer will retry processing this message with exponential backoff.
 */
public class TransientEventProcessingException extends RuntimeException {

    public TransientEventProcessingException(String message) {
        super(message);
    }

    public TransientEventProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
