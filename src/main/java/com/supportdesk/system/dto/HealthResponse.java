package com.supportdesk.system.dto;

import java.time.Instant;

public record HealthResponse(
        String application,
        String status,
        String database,
        String schemaVersion,
        Instant serverTime) {
}
