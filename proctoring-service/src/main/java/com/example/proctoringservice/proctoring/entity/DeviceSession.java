package com.example.proctoringservice.proctoring.entity;

import com.example.proctoringservice.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * One device connected to a proctoring session — normally the exam machine, optionally a phone
 * providing a second camera angle. Many rows per session rather than columns on the session
 * because a reconnect, a browser change or a second device each need their own environment record.
 * Two overlapping rows with different fingerprints is a strong cheating signal.
 */
@Entity
@Table(
        name = "device_sessions",
        indexes = {
                @Index(name = "ix_device_sessions_session", columnList = "proctoring_session_id"),
                @Index(name = "ix_device_sessions_fingerprint", columnList = "device_fingerprint")
        })
@Getter
@Setter
public class DeviceSession extends BaseEntity {

    @Column(name = "proctoring_session_id", nullable = false)
    private Long proctoringSessionId;

    @Column(name = "device_fingerprint", nullable = false, length = 128)
    private String deviceFingerprint;

    @Column(name = "is_primary", nullable = false)
    private boolean primary = true;

    @Column(name = "device_label", length = 120)
    private String deviceLabel;

    @Column(name = "operating_system", length = 64)
    private String operatingSystem;

    @Column(name = "os_version", length = 32)
    private String osVersion;

    @Column(length = 64)
    private String browser;

    @Column(name = "browser_version", length = 32)
    private String browserVersion;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "screen_resolution", length = 32)
    private String screenResolution;

    @Column(name = "monitor_count")
    private Integer monitorCount;

    @Column(name = "camera_label", length = 150)
    private String cameraLabel;

    @Column(name = "microphone_label", length = 150)
    private String microphoneLabel;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "geo_country", length = 2)
    private String geoCountry;

    @Column(name = "geo_city", length = 100)
    private String geoCity;

    @Column(name = "timezone_offset_minutes")
    private Integer timezoneOffsetMinutes;

    @Column(name = "vpn_suspected", nullable = false)
    private boolean vpnSuspected = false;

    @Column(name = "virtual_machine_suspected", nullable = false)
    private boolean virtualMachineSuspected = false;

    @Column(name = "connected_at", nullable = false)
    private Instant connectedAt = Instant.now();

    @Column(name = "disconnected_at")
    private Instant disconnectedAt;
}
