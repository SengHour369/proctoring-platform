package com.example.examservice.common.exception;

/** A precondition or invariant from the exam-service business rules was violated. */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
