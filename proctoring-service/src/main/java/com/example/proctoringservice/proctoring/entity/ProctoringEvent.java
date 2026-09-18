package com.example.proctoringservice.proctoring.entity;

import com.example.proctoringservice.common.entity.BaseEntity;
import com.example.proctoringservice.proctoring.enums.AutoAction;
import com.example.proctoringservice.proctoring.enums.EventSeverity;
import com.example.proctoringservice.proctoring.enums.EventSource;
import com.example.proctoringservice.proctoring.enums.ProctoringEventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * One observation during a session — a tab switch, a missed heartbeat, a proctor's manual flag, or
 * the summary of an AI finding. Append-only and the highest-volume table in the model, hence the
 * running counters on the session and the {@code (session, occurred_at)} index.
 *
 * <p>{@code occurredAt} is the client clock and {@code receivedAt} the server clock; a wide gap is
 * itself suspicious, so both are kept. {@code offsetMs} positions the event on the attempt
 * timeline, which is what a reviewer scrubs through.
 */
@Entity
@Table(
        name = "proctoring_events",
        indexes = {
                @Index(name = "ix_proctoring_events_session_time", columnList = "proctoring_session_id, occurred_at"),
                @Index(name = "ix_proctoring_events_type", columnList = "event_type"),
                @Index(name = "ix_proctoring_events_severity", columnList = "severity")
        })
@Getter
@Setter
public class ProctoringEvent extends BaseEntity {

    @Column(name = "proctoring_session_id", nullable = false)
    private Long proctoringSessionId;

    /** Which device raised it, when more than one was connected. */
    @Column(name = "device_session_id")
    private Long deviceSessionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 48)
    private ProctoringEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EventSeverity severity = EventSeverity.INFO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private EventSource source = EventSource.BROWSER_AGENT;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt = Instant.now();

    /** Milliseconds from attempt start, for timeline playback. */
    @Column(name = "offset_ms")
    private Long offsetMs;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(length = 500)
    private String description;

    /** Type-specific detail (tab URL host, key pressed, device ids…). Shape varies by event type. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "auto_action", nullable = false, length = 32)
    private AutoAction autoAction = AutoAction.NONE;

    /** Set when a proctor or reviewer dismisses the event as benign. */
    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @Column(name = "idempotency_key", length = 64)
    private String idempotencyKey;
}
