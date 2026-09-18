package com.example.examservice.common.exception;

/** Thrown when a caller lacks the permission required for an action. The caller-facing message
 * intentionally never reveals which permission was missing. */
public class AccessDeniedException extends RuntimeException {

    public AccessDeniedException(String message) {
        super(message);
    }
}
