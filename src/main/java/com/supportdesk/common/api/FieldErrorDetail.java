package com.supportdesk.common.api;

/** One field-level validation problem, e.g. {"field": "email", "message": "must be a valid email"}. */
public record FieldErrorDetail(String field, String message) {
}
