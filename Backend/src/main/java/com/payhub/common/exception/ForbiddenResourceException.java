package com.payhub.common.exception;

/**
 * Thrown when an authenticated user attempts to access a resource owned by
 * another tenant. Maps to HTTP 403.
 */
public class ForbiddenResourceException extends RuntimeException {

    public ForbiddenResourceException(String message) {
        super(message);
    }
}
