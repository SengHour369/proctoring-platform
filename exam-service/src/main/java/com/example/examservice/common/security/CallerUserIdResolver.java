package com.example.examservice.common.security;

import com.example.examservice.common.exception.AccessDeniedException;

/** Reads the calling user id off {@link CallerContextHolder} for controllers. */
public final class CallerUserIdResolver {

    private CallerUserIdResolver() {
    }

    public static Long require() {
        CallerContext context = CallerContextHolder.get();
        if (context == null || context.userId() == null) {
            throw new AccessDeniedException("caller identity is required");
        }
        return context.userId();
    }
}
