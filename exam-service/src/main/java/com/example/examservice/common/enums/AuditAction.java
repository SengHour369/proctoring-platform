package com.example.examservice.common.enums;

/** Kind of change an {@code AuditLog} row records. */
public enum AuditAction {
    CREATE,
    UPDATE,
    DELETE,
    PUBLISH,
    ASSIGN,
    CANCEL,
    CONFIG_CHANGE,
    PERMISSION_CHANGE
}
