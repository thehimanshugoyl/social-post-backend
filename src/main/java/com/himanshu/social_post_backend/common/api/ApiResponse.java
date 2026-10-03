package com.himanshu.social_post_backend.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.time.Instant;

/**
 * Standard generic response envelope for all successful API operations.
 *
 * @param <T> the type of the payload data
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        int status,
        String message,
        T data,
        Instant timestamp
) {
    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, HttpStatus.OK.value(), message, data, Instant.now());
    }

    public static <T> ApiResponse<T> ok(T data) {
        return ok(data, "Operation completed successfully");
    }

    public static <T> ApiResponse<T> created(T data, String message) {
        return new ApiResponse<>(true, HttpStatus.CREATED.value(), message, data, Instant.now());
    }

    public static <T> ApiResponse<T> created(T data) {
        return created(data, "Resource created successfully");
    }

    public static ApiResponse<Void> noContent(String message) {
        return new ApiResponse<>(true, HttpStatus.NO_CONTENT.value(), message, null, Instant.now());
    }
}
