package com.example.proctoringservice.proctoring.entity;

import com.example.proctoringservice.common.entity.BaseEntity;
import com.example.proctoringservice.proctoring.enums.CandidateLiveStatus;
import com.example.proctoringservice.proctoring.enums.ConnectionStatus;
import com.example.proctoringservice.proctoring.enums.StreamStatus;
import com.example.proctoringservice.sharedenums.RiskLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Current live state of one session, as the invigilator's wall of tiles reads it: camera, mic and
 * screen health, connection quality, latest risk score, question the candidate is on.
 *
 * <p>Its own table, one row per session, updated in place. The alternative — columns on
 * PROCTORING_SESSIONS — would have every heartbeat rewrite the row that events, evidence and
 * detections all hold foreign keys into, and no history is wanted here: this is a projection of
 * the latest known state, with the durable trail living in PROCTORING_EVENTS.
 */
@Entity
@Table(
        name = "live_session_status",
        uniqueConstraints = @UniqueConstraint(name = "uk_live_session_status_session", columnNames = "proctoring_session_id"),
        indexes = {
                @Index(name = "ix_live_session_status_risk", columnList = "risk_level"),
                @Index(name = "ix_live_session_status_conn", columnList = "connection_status"),
                @Index(name = "ix_live_session_status_candidate", columnList = "candidate_status")
        })
@Getter
@Setter
public class LiveSessionStatus extends BaseEntity {

    @Column(name = "proctoring_session_id", nullable = false)
    private Long proctoringSessionId;

    /** The candidate's own behavioral state — distinct from the stream/connection health below. */
    @Enumerated(EnumType.STRING)
    @Column(name = "candidate_status", nullable = false, length = 16)
    private CandidateLiveStatus candidateStatus = CandidateLiveStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "connection_status", nullable = false, length = 16)
    private ConnectionStatus connectionStatus = ConnectionStatus.CONNECTED;

    @Enumerated(EnumType.STRING)
    @Column(name = "camera_status", nullable = false, length = 16)
    private StreamStatus cameraStatus = StreamStatus.UNAVAILABLE;

    @Enumerated(EnumType.STRING)
    @Column(name = "microphone_status", nullable = false, length = 16)
    private StreamStatus microphoneStatus = StreamStatus.UNAVAILABLE;

    @Enumerated(EnumType.STRING)
    @Column(name = "screen_status", nullable = false, length = 16)
    private StreamStatus screenStatus = StreamStatus.UNAVAILABLE;

    @Column(name = "is_fullscreen", nullable = false)
    private boolean fullscreen = false;

    @Column(name = "is_window_focused", nullable = false)
    private boolean windowFocused = true;

    /** Mirrored from the latest RISK_ASSESSMENTS row so the monitor wall reads one table. */
    @Column(name = "risk_score", precision = 5, scale = 2)
    private BigDecimal riskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", length = 16)
    private RiskLevel riskLevel;

    @Column(name = "open_alert_count", nullable = false)
    private int openAlertCount = 0;

    @Column(name = "current_question_no")
    private Integer currentQuestionNo;

    @Column(name = "answered_count", nullable = false)
    private int answeredCount = 0;

    @Column(name = "remaining_seconds")
    private Integer remainingSeconds;

    @Column(name = "last_heartbeat_at")
    private Instant lastHeartbeatAt;

    @Column(name = "last_event_at")
    private Instant lastEventAt;

    @Column(name = "network_latency_ms")
    private Integer networkLatencyMs;

    @Column(name = "packet_loss_percent", precision = 5, scale = 2)
    private BigDecimal packetLossPercent;

    @Column(name = "network_bandwidth_mbps", precision = 8, scale = 2)
    private BigDecimal networkBandwidthMbps;

    @Column(name = "websocket_id", length = 128)
    private String websocketId;

    @Column(name = "updated_by_node", length = 64)
    private String updatedByNode;
}
