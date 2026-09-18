package com.example.examservice.exam.entity;

import com.example.examservice.exam.enums.ExcelRecalcMode;
import com.example.examservice.exam.enums.ExcelUiActionPolicy;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.Setter;

/**
 * How an exam's embedded Excel runtime behaves, for exams carrying one or more SPREADSHEET
 * questions. Modelled as a value object embedded in EXAMS, the same reasoning as
 * {@link ProctoringPolicy}: this policy has no identity or lifecycle apart from the exam that
 * declares it.
 *
 * <p>Deployment-level Excel concerns — which runtime engines are installed, per-session resource
 * quotas, the global allowed-functions/add-ins whitelist — are deliberately not here: those are
 * infrastructure, not exam policy, and live in {@code SystemSetting} instead, the same way a
 * global default lives there rather than in every exam's own row.
 */
@Embeddable
@Getter
@Setter
public class ExcelPolicy {

    @Column(name = "excel_runtime_required", nullable = false)
    private boolean runtimeRequired = false;

    /** Flags a candidate who opens the workbook in desktop Excel/Sheets/LibreOffice outside this runtime. */
    @Column(name = "excel_external_app_blocked", nullable = false)
    private boolean externalAppBlocked = true;

    /** Gate for every SPREADSHEET question's own {@code Question.excelMacroPolicy} — OFF here means no macro runs regardless of the question's setting. */
    @Column(name = "excel_macros_allowed", nullable = false)
    private boolean macrosAllowed = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "excel_copy_paste_policy", nullable = false, length = 16)
    private ExcelUiActionPolicy copyPastePolicy = ExcelUiActionPolicy.LOG;

    @Enumerated(EnumType.STRING)
    @Column(name = "excel_cut_drag_fill_policy", nullable = false, length = 16)
    private ExcelUiActionPolicy cutDragFillPolicy = ExcelUiActionPolicy.ALLOW;

    /** Seconds between cell-level autosaves. Null falls back to the system default. */
    @Column(name = "excel_autosave_interval_seconds")
    private Integer autosaveIntervalSeconds;

    /** Minutes between full workbook snapshots. Null falls back to the system default. */
    @Column(name = "excel_snapshot_interval_minutes")
    private Integer snapshotIntervalMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "excel_recalc_mode", nullable = false, length = 16)
    private ExcelRecalcMode recalcMode = ExcelRecalcMode.AUTOMATIC;

    @Column(name = "excel_iterative_calc_max_iterations")
    private Integer iterativeCalcMaxIterations;

    @Column(name = "excel_precision_as_displayed", nullable = false)
    private boolean precisionAsDisplayed = false;

    /** Pins NOW()/TODAY()/RAND()-family functions to a per-session seed instead of letting them vary on recalculation. */
    @Column(name = "excel_volatile_functions_pinned", nullable = false)
    private boolean volatileFunctionsPinned = true;
}
