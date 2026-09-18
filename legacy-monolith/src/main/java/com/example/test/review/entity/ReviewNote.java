package com.example.test.review.entity;

import com.example.test.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * A comment on a case — the working conversation between reviewers, separate from the
 * {@link ReviewDecision} rows that carry formal outcomes.
 *
 * <p>{@code candidateVisible} matters: an internal note ("second reviewer disagrees") and a note
 * quoted to the candidate in an appeal are held to different standards, so the boundary is a
 * column and not a convention.
 */
@Entity
@Table(
        name = "review_notes",
        indexes = @Index(name = "ix_review_notes_case", columnList = "review_case_id, created_at"))
@Getter
@Setter
public class ReviewNote extends BaseEntity {

    @Column(name = "review_case_id", nullable = false)
    private Long reviewCaseId;

    @Column(name = "author_user_id", nullable = false)
    private Long authorUserId;

    @Column(nullable = false, length = 4000)
    private String body;

    @Column(name = "candidate_visible", nullable = false)
    private boolean candidateVisible = false;

    /** Attachment supporting the note (an external report, a screenshot annotation). */
    @Column(name = "attachment_path", length = 512)
    private String attachmentPath;

    @Column(name = "edited_at")
    private Instant editedAt;
}
