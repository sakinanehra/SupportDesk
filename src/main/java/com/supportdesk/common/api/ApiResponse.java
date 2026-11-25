package com.supportdesk.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Standard envelope for every API response (SRS section 10).
 * The "errors" array is only present for validation failures.
 */
public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        Instant timestamp,
        @JsonInclude(JsonInclude.Include.NON_EMPTY) List<FieldErrorDetail> errors) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "Request successful.", data, Instant.now(), null);
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data, Instant.now(), null);
    }

    public static ApiResponse<Void> error(String message) {
        return new ApiResponse<>(false, message, null, Instant.now(), null);
    }

    public static ApiResponse<Void> error(String message, List<FieldErrorDetail> errors) {
        return new ApiResponse<>(false, message, null, Instant.now(), errors);
    }
}
