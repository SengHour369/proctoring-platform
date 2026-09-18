package com.example.examservice.exam.entity;

import com.example.examservice.common.entity.BaseEntity;
import com.example.examservice.exam.enums.InvitationChannel;
import com.example.examservice.exam.enums.InvitationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * One invitation sent for one assignment. Distinct from the assignment because invitations are
 * resent — a bounced address, a reminder, a changed sitting window — and each send has its own
 * token, channel and delivery state, while the entitlement itself is unchanged.
 */
@Entity
@Table(
        name = "exam_invitations",
        uniqueConstraints = @UniqueConstraint(name = "uk_exam_invitations_token", columnNames = "token_hash"),
        indexes = {
                @Index(name = "ix_exam_invitations_assignment", columnList = "exam_assignment_id"),
                @Index(name = "ix_exam_invitations_status", columnList = "status")
        })
@Getter
@Setter
public class ExamInvitation extends BaseEntity {

    @Column(name = "exam_assignment_id", nullable = false)
    private Long examAssignmentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private InvitationChannel channel = InvitationChannel.EMAIL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private InvitationStatus status = InvitationStatus.PENDING;

    /** Address or number actually used, kept for bounce diagnosis after a profile change. */
    @Column(name = "sent_to", nullable = false, length = 255)
    private String sentTo;

    @Column(name = "token_hash", length = 128)
    private String tokenHash;

    @Column(name = "sequence_no", nullable = false)
    private int sequenceNo = 1;

    @Column(name = "is_reminder", nullable = false)
    private boolean reminder = false;

    /**
     * Minutes before the exam window this reminder fires at (e.g. 1440 for 24h, 60 for 1h) —
     * null for the original invitation. Lets the reminder scheduler tell which offsets have
     * already fired for an assignment without re-deriving it from {@code sentAt}.
     */
    @Column(name = "reminder_offset_minutes")
    private Integer reminderOffsetMinutes;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "opened_at")
    private Instant openedAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;
}
