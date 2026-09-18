package com.example.test.attempt.entity;

import com.example.test.attempt.enums.QuestionStateType;
import com.example.test.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Delivery and navigation state of one question within one attempt: the shuffled order this
 * candidate saw it in, whether it has been viewed, how long it was on screen.
 *
 * <p>Separate from {@link AttemptAnswer} on purpose. An answer is what the candidate submitted; a
 * state row exists from the moment the paper is generated, including for questions never answered
 * — which is exactly the signal a reviewer wants when explaining a suspicious timing pattern.
 */
@Entity
@Table(
        name = "question_states",
        uniqueConstraints = @UniqueConstraint(name = "uk_question_states", columnNames = {"exam_attempt_id", "exam_question_id"}))
@Getter
@Setter
public class QuestionState extends BaseEntity {

    @Column(name = "exam_attempt_id", nullable = false)
    private Long examAttemptId;

    @Column(name = "exam_question_id", nullable = false)
    private Long examQuestionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private QuestionStateType state = QuestionStateType.UNSEEN;

    /** Position in the shuffled paper this candidate received. */
    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    /** Order the options were rendered in, when the placement shuffles them. */
    @Column(name = "option_order", length = 255)
    private String optionOrder;

    @Column(name = "first_viewed_at")
    private Instant firstViewedAt;

    @Column(name = "last_viewed_at")
    private Instant lastViewedAt;

    @Column(name = "view_count", nullable = false)
    private int viewCount = 0;

    @Column(name = "time_on_question_seconds", nullable = false)
    private int timeOnQuestionSeconds = 0;

    @Column(name = "locked_at")
    private Instant lockedAt;
}
