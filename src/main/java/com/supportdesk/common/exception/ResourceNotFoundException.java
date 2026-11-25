package com.supportdesk.common.exception;

import org.springframework.http.HttpStatus;

/** 404: resource does not exist OR is not accessible to the caller (we never reveal which). */
public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
