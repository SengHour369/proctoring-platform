package com.example.identityservice.auth.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Serializes the before/after maps {@link AuditWriter} writes into {@code AuditLog}'s JSON columns. */
@Component
public class JsonSnapshot {

    private final ObjectMapper objectMapper;

    public JsonSnapshot(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String of(Map<String, Object> fields) {
        if (fields == null || fields.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(fields);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize audit snapshot", e);
        }
    }
}
