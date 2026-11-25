package com.supportdesk.common.exception;

import org.springframework.http.HttpStatus;

/** 409: duplicate or conflicting operation (e.g. email already registered). */
public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
