package com.supportdesk.system.service;

import com.supportdesk.system.dto.HealthResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class SystemService {

    private static final Logger log = LoggerFactory.getLogger(SystemService.class);

    private final JdbcTemplate jdbcTemplate;
    private final String applicationName;

    public SystemService(JdbcTemplate jdbcTemplate,
                         @Value("${spring.application.name}") String applicationName) {
        this.jdbcTemplate = jdbcTemplate;
        this.applicationName = applicationName;
    }

    public HealthResponse checkHealth() {
        boolean databaseUp = isDatabaseUp();
        String schemaVersion = databaseUp ? latestSchemaVersion() : "unknown";
        return new HealthResponse(
                applicationName,
                databaseUp ? "UP" : "DEGRADED",
                databaseUp ? "UP" : "DOWN",
                schemaVersion,
                Instant.now());
    }

    private boolean isDatabaseUp() {
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return Integer.valueOf(1).equals(result);
        } catch (DataAccessException ex) {
            log.warn("Database health check failed: {}", ex.getMessage());
            return false;
        }
    }

    /** Reads the last successfully applied Flyway migration, proving the schema was created. */
    private String latestSchemaVersion() {
        try {
            List<String> versions = jdbcTemplate.queryForList(
                    "SELECT version FROM flyway_schema_history WHERE success = 1 "
                            + "ORDER BY installed_rank DESC LIMIT 1", String.class);
            return versions.isEmpty() ? "none" : versions.get(0);
        } catch (DataAccessException ex) {
            log.warn("Could not read Flyway history: {}", ex.getMessage());
            return "unknown";
        }
    }
}
