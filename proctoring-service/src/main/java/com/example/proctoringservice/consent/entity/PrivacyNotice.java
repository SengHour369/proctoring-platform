package com.example.proctoringservice.consent.entity;

import com.example.proctoringservice.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * One version of the privacy/consent text a candidate must accept before a proctored sitting.
 * Effective-dated and never edited in place, for the same reason a risk-factor weight isn't: a
 * model or data-handling change big enough to need a new notice must not silently apply to a
 * consent someone already gave under the old wording.
 */
@Entity
@Table(
        name = "privacy_notices",
        uniqueConstraints = @UniqueConstraint(name = "uk_privacy_notices_version", columnNames = {"notice_code", "version", "locale"}))
@Getter
@Setter
public class PrivacyNotice extends BaseEntity {

    @Column(name = "notice_code", nullable = false, length = 64)
    private String noticeCode;

    @Column(nullable = false, length = 32)
    private String version;

    @Column(nullable = false, length = 8)
    private String locale;

    @Column(name = "body_markdown", nullable = false, length = 8000)
    private String bodyMarkdown;

    @Column(name = "effective_from", nullable = false)
    private Instant effectiveFrom = Instant.now();

    @Column(name = "effective_to")
    private Instant effectiveTo;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
