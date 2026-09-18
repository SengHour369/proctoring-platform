package com.example.identityservice.auth.dto;

/** Where a request came from — carried through to LoginAttempt/SecurityToken/AuditLog rows. */
public record RequestContext(String ipAddress, String userAgent, String deviceFingerprint, String geoCountry) {
}
