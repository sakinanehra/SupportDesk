package com.supportdesk.system.controller;

import com.supportdesk.common.api.ApiResponse;
import com.supportdesk.common.exception.BusinessRuleException;
import com.supportdesk.common.exception.ConflictException;
import com.supportdesk.common.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exists ONLY in the "dev" profile. It lets us prove the error-handling pipeline works
 * before real features exist. It is never loaded in production.
 */
@RestController
@RequestMapping("/api/dev")
@Profile("dev")
public class DevDiagnosticsController {

    @GetMapping("/errors/{type}")
    public ApiResponse<Void> triggerError(@PathVariable String type) {
        switch (type) {
            case "not-found" -> throw new ResourceNotFoundException("Ticket not found.");
            case "conflict" -> throw new ConflictException("An account with this email already exists.");
            case "business-rule" ->
                    throw new BusinessRuleException("Invalid status transition: OPEN to RESOLVED is not allowed.");
            case "server-error" ->
                    throw new IllegalStateException("Simulated crash. internal detail: db.password=hunter2");
            default -> {
                // fall through to the success response below
            }
        }
        return ApiResponse.ok("No error was triggered for type '" + type + "'.", null);
    }

    @PostMapping("/validate")
    public ApiResponse<DemoRequest> validate(@Valid @RequestBody DemoRequest request) {
        return ApiResponse.ok("Validation passed.", request);
    }

    public record DemoRequest(
            @NotBlank(message = "Name is required")
            @Size(max = 50, message = "Name must be at most 50 characters")
            String name,

            @NotBlank(message = "Email is required")
            @Email(message = "Email must be a valid email address")
            String email) {
    }
}
