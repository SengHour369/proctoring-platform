package com.example.identityservice.auth.exception;

/**
 * A uniqueness constraint on {@code users} would be violated. The message is deliberately generic
 * — it must not reveal whether the existing row is a student, a teacher or anything else.
 */
public class DuplicateValueException extends RuntimeException {

    public DuplicateValueException(String message) {
        super(message);
    }
}
