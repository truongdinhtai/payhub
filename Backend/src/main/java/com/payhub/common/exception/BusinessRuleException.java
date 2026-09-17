package com.payhub.common.exception;

/**
 * Thrown when an operation violates a domain/business rule (e.g. downgrading to
 * a plan whose limits the account already exceeds). Maps to HTTP 409.
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
