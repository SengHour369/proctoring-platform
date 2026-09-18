package com.example.resultservice.result.entity;

import com.example.resultservice.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Per-question line of a result: the frozen scoring breakdown behind the total.
 *
 * <p>It duplicates what could be recomputed by joining answers to questions, and that is the
 * point. Points per question, negative marking and partial credit are all configurable, so a
 * result published in March must not change when the exam's weights are retuned in April. This is
 * the snapshot the candidate was shown and the reviewer argued over.
 */
@Entity
@Table(
        name = "result_details",
        uniqueConstraints = @UniqueConstraint(name = "uk_result_details_question", columnNames = {"exam_result_id", "exam_question_id"}))
@Getter
@Setter
public class ResultDetail extends BaseEntity {

    @Column(name = "exam_result_id", nullable = false)
    private Long examResultId;

    @Column(name = "exam_question_id", nullable = false)
    private Long examQuestionId;

    /** Denormalised from the placement so section subtotals need no extra join. */
    @Column(name = "exam_section_id")
    private Long examSectionId;

    /** Null when the question was never answered — the row still records the points forgone. */
    @Column(name = "attempt_answer_id")
    private Long attemptAnswerId;

    @Column(name = "sequence_no", nullable = false)
    private int sequenceNo;

    @Column(name = "points_awarded", nullable = false, precision = 9, scale = 2)
    private BigDecimal pointsAwarded = BigDecimal.ZERO;

    @Column(name = "points_possible", nullable = false, precision = 9, scale = 2)
    private BigDecimal pointsPossible;

    @Column(name = "is_correct")
    private Boolean correct;

    @Column(name = "answered", nullable = false)
    private boolean answered = false;

    @Column(name = "time_spent_seconds")
    private Integer timeSpentSeconds;

    @Column(name = "scoring_note", length = 500)
    private String scoringNote;
}
