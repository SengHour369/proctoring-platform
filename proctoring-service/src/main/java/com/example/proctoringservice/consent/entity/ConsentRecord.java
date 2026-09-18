package com.example.proctoringservice.consent.entity;

import com.example.proctoringservice.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * One candidate's acceptance of one {@link PrivacyNotice} version for one attempt. Required
 * before {@code ExamAttemptService.startAttempt} proceeds whenever the exam's
 * {@code ProctoringPolicy.mode} is anything other than NONE — a proctored sitting without
 * recorded consent is a legal defect, not merely a missing row.
 *
 * <p>{@code noticeVersion} is copied at the moment of consent, so a later edit to the notice
 * text can never change what a candidate is deemed to have agreed to. Withdrawal sets
 * {@code withdrawnAt} but never deletes the row — the history of consent given and withdrawn is
 * itself the record a dispute would turn on.
 */
@Entity
@Table(
        name = "consent_records",
        indexes = @Index(name = "ix_consent_records_attempt", columnList = "exam_attempt_id"))
@Getter
@Setter
public class ConsentRecord extends BaseEntity {

    @Column(name = "exam_attempt_id", nullable = false)
    private Long examAttemptId;

    @Column(name = "privacy_notice_id", nullable = false)
    private Long privacyNoticeId;

    @Column(name = "notice_version", nullable = false, length = 32)
    private String noticeVersion;

    @Column(name = "consented_at", nullable = false)
    private Instant consentedAt = Instant.now();

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "withdrawn_at")
    private Instant withdrawnAt;
}
