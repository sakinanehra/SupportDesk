package com.supportdesk.system.controller;

import com.supportdesk.common.api.ApiResponse;
import com.supportdesk.system.dto.HealthResponse;
import com.supportdesk.system.service.SystemService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
public class SystemController {

    private final SystemService systemService;

    public SystemController(SystemService systemService) {
        this.systemService = systemService;
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<HealthResponse>> health() {
        HealthResponse health = systemService.checkHealth();
        boolean up = "UP".equals(health.status());
        return ResponseEntity
                .status(up ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.ok(up ? "Service is healthy." : "Service is degraded.", health));
    }
}
