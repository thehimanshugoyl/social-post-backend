package com.himanshu.social_post_backend.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

/**
 * Standard error response envelope providing structured, actionable feedback to API consumers
 * with integrated correlation ID tracing.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiErrorResponse(
        boolean success,
        int status,
        String error,
        String message,
        String path,
        String correlationId,
        Instant timestamp,
        List<ValidationErrorItem> validationErrors
) {
    public record ValidationErrorItem(
            String field,
            Object rejectedValue,
            String message
    ) {}

    public static ApiErrorResponse of(HttpStatus status, String message, String path, List<ValidationErrorItem> validationErrors) {
        String correlationId = MDC.get("correlationId");
        return new ApiErrorResponse(
                false,
                status.value(),
                status.getReasonPhrase(),
                message,
                path,
                correlationId,
                Instant.now(),
                validationErrors != null ? validationErrors : List.of()
        );
    }

    public static ApiErrorResponse of(HttpStatus status, String message, String path) {
        return of(status, message, path, List.of());
    }
}
