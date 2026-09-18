package com.example.proctoringservice.proctoring.entity;

import com.example.proctoringservice.common.entity.BaseEntity;
import com.example.proctoringservice.proctoring.enums.ProctorShiftOutcome;
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
 * One proctor's assignment window over one session, in LIVE_PROCTOR/HYBRID mode where coverage
 * rotates across a shift change. {@code ProctoringSession.assignedProctorUserId} is a mirror of
 * whichever shift is currently open, not the source of truth — this table is, since overwriting
 * that single column on handover would lose who was watching when.
 *
 * <p>At most one shift per session may have a null {@code shiftEndAt} at any time: the one
 * currently on duty.
 */
@Entity
@Table(
        name = "proctor_shifts",
        indexes = {
                @Index(name = "ix_proctor_shifts_session", columnList = "proctoring_session_id"),
                @Index(name = "ix_proctor_shifts_open", columnList = "proctoring_session_id, shift_end_at")
        })
@Getter
@Setter
public class ProctorShift extends BaseEntity {

    @Column(name = "proctoring_session_id", nullable = false)
    private Long proctoringSessionId;

    @Column(name = "proctor_user_id", nullable = false)
    private Long proctorUserId;

    @Column(name = "shift_start_at", nullable = false)
    private Instant shiftStartAt = Instant.now();

    /** Null while this proctor is the one currently on duty. */
    @Column(name = "shift_end_at")
    private Instant shiftEndAt;

    /** What the outgoing proctor wants the incoming one to watch. Required on a handover. */
    @Column(name = "handover_note", length = 1000)
    private String handoverNote;

    @Column(name = "events_during_shift", nullable = false)
    private int eventsDuringShift = 0;

    @Column(name = "flags_raised_during_shift", nullable = false)
    private int flagsRaisedDuringShift = 0;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private ProctorShiftOutcome outcome;
}
