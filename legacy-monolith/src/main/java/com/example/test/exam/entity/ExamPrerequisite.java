package com.example.test.exam.entity;

import com.example.test.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * One eligibility rule gating an exam — "must have passed exam X at 70%" or "must have completed
 * course Y." One row per rule, since an exam can carry several independent ones; not an embedded
 * value object like {@link ProctoringPolicy}, because that pattern is for genuinely 1:1 objects
 * with no identity of their own, and prerequisites are 1:many.
 *
 * <p>Evaluating the rule against a candidate's history is service-layer logic — this table only
 * stores what the rule is.
 */
@Entity
@Table(
        name = "exam_prerequisites",
        indexes = {
                @Index(name = "ix_exam_prerequisites_exam", columnList = "exam_id"),
                @Index(name = "ix_exam_prerequisites_required_exam", columnList = "required_exam_id")
        })
@Getter
@Setter
public class ExamPrerequisite extends BaseEntity {

    @Column(name = "exam_id", nullable = false)
    private Long examId;

    /** Null when the rule is coursework-based rather than another exam. */
    @Column(name = "required_exam_id")
    private Long requiredExamId;

    @Column(name = "min_score", precision = 9, scale = 2)
    private BigDecimal minScore;

    /** External LMS/coursework code, same shape as User.externalRef. */
    @Column(name = "course_reference", length = 150)
    private String courseReference;

    /** Shown to a candidate who fails the check. */
    @Column(length = 500)
    private String description;

    /** Retire a rule without losing the history of what gated past assignments. */
    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
