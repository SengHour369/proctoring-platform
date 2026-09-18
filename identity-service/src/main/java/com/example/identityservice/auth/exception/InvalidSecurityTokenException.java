package com.example.identityservice.auth.exception;

/** A security token was not found, wrong purpose, already used, invalidated, or expired. */
public class InvalidSecurityTokenException extends RuntimeException {

    public InvalidSecurityTokenException(String message) {
        super(message);
    }
}
