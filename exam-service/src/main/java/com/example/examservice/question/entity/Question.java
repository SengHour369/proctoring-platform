package com.example.examservice.question.entity;

import com.example.examservice.common.entity.BaseEntity;
import com.example.examservice.question.enums.ExcelMacroPolicy;
import com.example.examservice.question.enums.ExcelWorkbookFileType;
import com.example.examservice.question.enums.ProgrammingLanguage;
import com.example.examservice.question.enums.QuestionDifficulty;
import com.example.examservice.question.enums.QuestionStatus;
import com.example.examservice.question.enums.QuestionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Reusable item in the question bank, independent of any exam. Questions are never hard-deleted
 * once used — they are RETIRED — because old attempts and results must stay readable. Fixing or
 * improving one that's already been sat can't edit the row in place for the same reason, so a
 * new version is a new row: insert it with {@code parentQuestionId} pointing back at the one it
 * replaces and {@code version = parent.version + 1}, then retire the parent once the new version
 * is placed into exams. Every past {@code ExamQuestion}/{@code AttemptAnswer} keeps referencing
 * the exact row a candidate actually saw — nothing needs re-authoring from scratch, and nothing
 * that already happened is rewritten.
 */
@Entity
@Table(
        name = "questions",
        uniqueConstraints = @UniqueConstraint(name = "uk_questions_code", columnNames = "code"),
        indexes = {
                @Index(name = "ix_questions_type", columnList = "question_type"),
                @Index(name = "ix_questions_status", columnList = "status"),
                @Index(name = "ix_questions_topic", columnList = "topic"),
                @Index(name = "ix_questions_parent", columnList = "parent_question_id")
        })
@Getter
@Setter
public class Question extends BaseEntity {

    @Column(length = 64)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 32)
    private QuestionType questionType;

    /** The prompt shown to the candidate. */
    @Column(name = "stem", nullable = false, length = 4000)
    private String stem;

    /** Shown after grading, not during the attempt. */
    @Column(length = 2000)
    private String explanation;

    /** Expected answer for auto-graded free-text and numeric types. */
    @Column(name = "answer_key", length = 2000)
    private String answerKey;

    /** Tolerance for NUMERIC answers, e.g. 0.01. */
    @Column(name = "numeric_tolerance", precision = 12, scale = 6)
    private BigDecimal numericTolerance;

    @Column(name = "media_path", length = 512)
    private String mediaPath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private QuestionDifficulty difficulty = QuestionDifficulty.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private QuestionStatus status = QuestionStatus.DRAFT;

    @Column(name = "default_points", nullable = false, precision = 9, scale = 2)
    private BigDecimal defaultPoints = BigDecimal.ONE;

    @Column(length = 120)
    private String topic;

    /** Subject placement in the bank taxonomy; drives pool draws by subject area. */
    @Column(name = "question_category_id")
    private Long questionCategoryId;

    @Column(name = "expected_seconds")
    private Integer expectedSeconds;

    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    @Column(name = "version", nullable = false)
    private int version = 1;

    /**
     * The question this one is a new version of, when it was created by inheriting from an
     * existing item rather than authored from scratch. Null for an original question that has no
     * predecessor.
     */
    @Column(name = "parent_question_id")
    private Long parentQuestionId;

    /*
     * The four fields below are only meaningful when questionType = CODE, the same way
     * numericTolerance only matters for NUMERIC and answerKey only for the auto-graded free-text
     * types — one bank item, one row, most columns unused for any given type.
     */

    @Enumerated(EnumType.STRING)
    @Column(name = "programming_language", length = 16)
    private ProgrammingLanguage programmingLanguage;

    @Column(name = "starter_code", length = 8000)
    private String starterCode;

    @Column(name = "execution_time_limit_ms")
    private Integer executionTimeLimitMs;

    @Column(name = "execution_memory_limit_mb")
    private Integer executionMemoryLimitMb;

    /*
     * The eight fields below are only meaningful when questionType = SPREADSHEET. Re-uploading a
     * corrected workbook for a question already sat by a candidate is the same act as any other
     * question fix — it goes through createNextVersion (parentQuestionId/version above), not an
     * in-place edit of these columns, so a workbook version has no separate version table of its
     * own to duplicate that mechanism.
     */

    @Enumerated(EnumType.STRING)
    @Column(name = "workbook_file_type", length = 16)
    private ExcelWorkbookFileType workbookFileType;

    @Column(name = "workbook_storage_path", length = 512)
    private String workbookStoragePath;

    /** Tamper check for the authored template, the same reasoning as EvidenceFile.checksumSha256. */
    @Column(name = "workbook_checksum_sha256", length = 64)
    private String workbookChecksumSha256;

    @Column(name = "workbook_size_bytes")
    private Long workbookSizeBytes;

    @Column(name = "workbook_sheet_count")
    private Integer workbookSheetCount;

    @Column(name = "workbook_has_macros", nullable = false)
    private boolean workbookHasMacros = false;

    @Column(name = "workbook_has_external_links", nullable = false)
    private boolean workbookHasExternalLinks = false;

    /** Only enforceable when the owning exam's ExcelPolicy.macrosAllowed is also true. */
    @Enumerated(EnumType.STRING)
    @Column(name = "excel_macro_policy", length = 24)
    private ExcelMacroPolicy excelMacroPolicy;
}
