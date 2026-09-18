package com.example.attemptservice.attempt.entity;

import com.example.attemptservice.common.entity.BaseEntity;
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
 * A hash of the shuffled paper one attempt actually received, computed once from its persisted
 * {@link QuestionState} rows at start. Two attempts in the same exam sharing a hash is a strong
 * collusion signal — or a broken shuffler — and something a shuffle *rule* can never surface,
 * because two candidates can independently draw the same rule and land on different forms.
 *
 * <p>Scoped to {@code examId}: the same hash appearing under a different exam means nothing.
 */
@Entity
@Table(
        name = "question_form_fingerprints",
        uniqueConstraints = @UniqueConstraint(name = "uk_question_form_fingerprints_attempt", columnNames = "exam_attempt_id"),
        indexes = @Index(name = "ix_question_form_fingerprints_hash", columnList = "exam_id, form_hash"))
@Getter
@Setter
public class QuestionFormFingerprint extends BaseEntity {

    @Column(name = "exam_attempt_id", nullable = false)
    private Long examAttemptId;

    @Column(name = "exam_id", nullable = false)
    private Long examId;

    /** SHA-256 over the ordered (examQuestionId, displayOrder, optionOrder) tuples delivered. */
    @Column(name = "form_hash", nullable = false, length = 64)
    private String formHash;

    @Column(name = "question_count", nullable = false)
    private int questionCount;

    @Column(name = "section_count", nullable = false)
    private int sectionCount;

    @Column(name = "computed_at", nullable = false)
    private Instant computedAt = Instant.now();

    @Column(name = "collision_count", nullable = false)
    private int collisionCount = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "collision_attempt_ids")
    private String collisionAttemptIds;
}
