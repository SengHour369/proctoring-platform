package com.example.examservice.common.security;

/** Thread-local holder for the current request's {@link CallerContext}, set by {@link CallerContextFilter}. */
public final class CallerContextHolder {

    private static final ThreadLocal<CallerContext> CURRENT = new ThreadLocal<>();

    private CallerContextHolder() {
    }

    public static void set(CallerContext context) {
        CURRENT.set(context);
    }

    public static CallerContext get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
