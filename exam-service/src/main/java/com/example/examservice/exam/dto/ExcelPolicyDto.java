package com.example.examservice.exam.dto;

import com.example.examservice.exam.enums.ExcelRecalcMode;
import com.example.examservice.exam.enums.ExcelUiActionPolicy;

/** Wire shape of {@code ExcelPolicy}. Nullable fields, same reasoning as {@link ProctoringPolicyDto}. */
public record ExcelPolicyDto(
        Boolean runtimeRequired,
        Boolean externalAppBlocked,
        Boolean macrosAllowed,
        ExcelUiActionPolicy copyPastePolicy,
        ExcelUiActionPolicy cutDragFillPolicy,
        Integer autosaveIntervalSeconds,
        Integer snapshotIntervalMinutes,
        ExcelRecalcMode recalcMode,
        Integer iterativeCalcMaxIterations,
        Boolean precisionAsDisplayed,
        Boolean volatileFunctionsPinned
) {
}
