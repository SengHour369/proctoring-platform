package com.example.identityservice.auth.exception;

/** A {@code Role} with {@code system = true} cannot be deleted or have its code changed. */
public class SystemRoleModificationException extends RuntimeException {

    public SystemRoleModificationException(String message) {
        super(message);
    }
}
