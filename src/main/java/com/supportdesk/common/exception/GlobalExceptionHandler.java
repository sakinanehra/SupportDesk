package com.supportdesk.common.exception;

import com.supportdesk.common.api.ApiResponse;
import com.supportdesk.common.api.FieldErrorDetail;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Single place where every exception becomes an ApiResponse.
 * Extending ResponseEntityExceptionHandler means Spring's own MVC errors
 * (malformed JSON, wrong HTTP method, unknown route, wrong content type...)
 * are also returned in our envelope instead of Spring's default format.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ---- Our own exceptions -------------------------------------------------

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException ex) {
        return ResponseEntity.status(ex.getStatus()).body(ApiResponse.error(ex.getMessage()));
    }

    // ---- Validation ---------------------------------------------------------

    /** Triggered by @Valid on a request body. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        List<FieldErrorDetail> errors = new ArrayList<>();
        ex.getBindingResult().getFieldErrors().forEach(fe ->
                errors.add(new FieldErrorDetail(fe.getField(), fe.getDefaultMessage())));
        ex.getBindingResult().getGlobalErrors().forEach(ge ->
                errors.add(new FieldErrorDetail(ge.getObjectName(), ge.getDefaultMessage())));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Validation failed. Please correct the highlighted fields.", errors));
    }

    /** Triggered by constraints on @PathVariable / @RequestParam in @Validated controllers. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        List<FieldErrorDetail> errors = ex.getConstraintViolations().stream()
                .map(v -> new FieldErrorDetail(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Validation failed. Please correct the request parameters.", errors));
    }

    // ---- Persistence --------------------------------------------------------

    /** Another user changed the same row first (@Version mismatch). */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Void>> handleOptimisticLock(OptimisticLockingFailureException ex) {
        log.warn("Optimistic locking conflict: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("This record was modified by someone else. Please refresh and try again."));
    }

    /** Safety net for DB constraint violations that slipped past service-level checks. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("The request conflicts with existing data."));
    }

    // ---- Catch-all ----------------------------------------------------------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        // Full details go to the server log only; the client gets a generic message (NFR-005).
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(messageFor(HttpStatus.INTERNAL_SERVER_ERROR)));
    }

    // ---- Spring MVC's built-in exceptions (405, 415, 404 route, malformed JSON...) ----

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        if (statusCode.is5xxServerError()) {
            log.error("Server error handled by MVC infrastructure", ex);
        }
        return ResponseEntity.status(statusCode).headers(headers)
                .body(ApiResponse.error(messageFor(statusCode)));
    }

    private String messageFor(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> "The request is invalid or malformed.";
            case 404 -> "The requested resource was not found.";
            case 405 -> "This HTTP method is not supported for the requested endpoint.";
            case 415 -> "Unsupported content type. Please send application/json.";
            default -> status.is5xxServerError()
                    ? "An unexpected error occurred. Please try again later."
                    : "The request could not be processed.";
        };
    }
}
