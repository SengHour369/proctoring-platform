package com.example.test.attempt.entity;

import com.example.test.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * Append-only history of one answer. {@link AttemptAnswer} holds the current response so scoring
 * stays a single-row read; every save — manual or auto-save — also lands here.
 *
 * <p>This is what recovers an answer after a crash mid-attempt, and what shows a reviewer that a
 * blank answer became correct in the four seconds after a tab switch. Selected options are stored
 * as a JSON snapshot rather than FK rows: history must not change if an option is later edited.
 */
@Entity
@Table(
        name = "answer_revisions",
        uniqueConstraints = @UniqueConstraint(name = "uk_answer_revisions", columnNames = {"attempt_answer_id", "revision_no"}),
        indexes = @Index(name = "ix_answer_revisions_saved", columnList = "attempt_answer_id, saved_at"))
@Getter
@Setter
public class AnswerRevision extends BaseEntity {

    @Column(name = "attempt_answer_id", nullable = false)
    private Long attemptAnswerId;

    @Column(name = "revision_no", nullable = false)
    private int revisionNo;

    @Column(name = "saved_at", nullable = false)
    private Instant savedAt = Instant.now();

    /** True when the platform saved on a timer rather than the candidate pressing save. */
    @Column(name = "auto_saved", nullable = false)
    private boolean autoSaved = false;

    @Column(name = "response_text", length = 8000)
    private String responseText;

    @Column(name = "response_numeric", precision = 18, scale = 6)
    private java.math.BigDecimal responseNumeric;

    /** Snapshot of the selected QUESTION_OPTIONS ids at this revision. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "selected_option_ids")
    private String selectedOptionIds;

    @Column(name = "client_timestamp")
    private Instant clientTimestamp;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;
}
