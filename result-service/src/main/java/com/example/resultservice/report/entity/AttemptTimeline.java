package com.example.resultservice.report.entity;

import com.example.resultservice.common.entity.BaseEntity;
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
 * A rendered, unified timeline for one attempt — proctoring events, answer revisions, risk
 * contributions, and evidence captures, ordered by their own offsets/timestamps, so a reviewer
 * doesn't join five tables by hand to scrub one sitting.
 *
 * <p>A snapshot like {@link ProctoringReport}, not a live view: a later event must not alter a
 * timeline already shown to a reviewer, so a re-render is a new {@code version}, and each entry
 * carries a {@code refId} back to its source row rather than duplicating that row's content —
 * this table is a rendering of existing data, never a second copy of it.
 */
@Entity
@Table(
        name = "attempt_timelines",
        uniqueConstraints = @UniqueConstraint(name = "uk_attempt_timelines_version", columnNames = {"exam_attempt_id", "version"}),
        indexes = @Index(name = "ix_attempt_timelines_attempt", columnList = "exam_attempt_id"))
@Getter
@Setter
public class AttemptTimeline extends BaseEntity {

    @Column(name = "exam_attempt_id", nullable = false)
    private Long examAttemptId;

    @Column(nullable = false)
    private int version = 1;

    @Column(name = "rendered_at", nullable = false)
    private Instant renderedAt = Instant.now();

    @Column(name = "entry_count", nullable = false)
    private int entryCount = 0;

    /** JSON array of {offsetMs, kind, refId, label, severity} — refId points back to its source row. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "timeline_json")
    private String timelineJson;

    @Column(name = "checksum_sha256", length = 64)
    private String checksumSha256;

    /** Null for an automatically rendered timeline. */
    @Column(name = "generated_by_user_id")
    private Long generatedByUserId;
}
