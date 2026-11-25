package com.supportdesk.common.exception;

import org.springframework.http.HttpStatus;

/** 422: the request is well-formed but violates a business rule (e.g. invalid status transition). */
public class BusinessRuleException extends ApiException {

    public BusinessRuleException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, message);
    }
}
