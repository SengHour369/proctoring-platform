package com.example.proctoringservice.proctoring.entity;

import com.example.proctoringservice.common.entity.BaseEntity;
import com.example.proctoringservice.proctoring.enums.ProctorActionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * A live human proctor's action on a session — warn, message, flag, pause, terminate — with the
 * same audit standard a reviewer's decision already gets. Writing only a
 * {@link ProctoringEvent} with {@code source = HUMAN_PROCTOR} would record *that* something
 * happened but not the authority behind it; this table is where the reason, and — for a
 * termination under a two-person-approval policy — the second signature, live.
 */
@Entity
@Table(
        name = "proctor_actions",
        indexes = {
                @Index(name = "ix_proctor_actions_session", columnList = "proctoring_session_id"),
                @Index(name = "ix_proctor_actions_shift", columnList = "proctor_shift_id")
        })
@Getter
@Setter
public class ProctorAction extends BaseEntity {

    @Column(name = "proctoring_session_id", nullable = false)
    private Long proctoringSessionId;

    @Column(name = "proctor_user_id", nullable = false)
    private Long proctorUserId;

    @Column(name = "proctor_shift_id")
    private Long proctorShiftId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 16)
    private ProctorActionType actionType;

    /** Required for WARN, FLAG, PAUSE, TERMINATE — a silent action is an unauditable one. */
    @Column(length = 500)
    private String reason;

    @Column(name = "proctoring_event_id")
    private Long proctoringEventId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt = Instant.now();

    @Column(name = "candidate_notified", nullable = false)
    private boolean candidateNotified = false;

    /** Required for TERMINATE when the exam's policy demands two-person approval. */
    @Column(name = "supervisor_approval_user_id")
    private Long supervisorApprovalUserId;

    @Column(name = "supervisor_approved_at")
    private Instant supervisorApprovedAt;
}
