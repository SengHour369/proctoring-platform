package com.example.identityservice.auth.exception;

/**
 * The caller does not hold the permission an operation requires. Distinct from a validation
 * failure: a denial is itself auditable, so every throw site writes a {@code DENIED} audit row
 * before raising this.
 */
public class AccessDeniedException extends RuntimeException {

    public AccessDeniedException(String message) {
        super(message);
    }
}
