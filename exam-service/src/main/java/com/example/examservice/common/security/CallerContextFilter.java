package com.example.examservice.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Populates {@link CallerContextHolder} from {@code X-User-Id} / {@code X-User-Permissions}
 * headers for the lifetime of the request. See {@link CallerContext} for why headers rather than
 * a verified token, for now.
 */
@Component
public class CallerContextFilter extends OncePerRequestFilter {

    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String PERMISSIONS_HEADER = "X-User-Permissions";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            Long userId = parseUserId(request.getHeader(USER_ID_HEADER));
            Set<String> permissions = parsePermissions(request.getHeader(PERMISSIONS_HEADER));
            CallerContextHolder.set(new CallerContext(userId, permissions));
            chain.doFilter(request, response);
        } finally {
            CallerContextHolder.clear();
        }
    }

    private Long parseUserId(String header) {
        if (header == null || header.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(header.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Set<String> parsePermissions(String header) {
        if (header == null || header.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(header.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }
}
