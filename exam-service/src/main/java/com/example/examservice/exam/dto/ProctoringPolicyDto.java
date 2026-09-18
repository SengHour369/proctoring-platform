package com.example.examservice.exam.dto;

import com.example.examservice.exam.enums.ProctoringMode;

/** Wire shape of {@code ProctoringPolicy}. Every field is nullable on input — a create request
 * that omits it falls back to the entity's own default, and an update only touches fields it
 * actually sets. */
public record ProctoringPolicyDto(
        ProctoringMode mode,
        Boolean requireWebcam,
        Boolean requireScreenShare,
        Boolean requireMicrophone,
        Boolean requireIdentityCheck,
        Boolean requireEnvironmentScan,
        Boolean forceFullscreen,
        Boolean monitorScreen,
        Boolean detectTabSwitch,
        Boolean detectFace,
        Boolean detectMultipleFaces,
        Boolean detectGaze,
        Boolean detectObjects,
        Boolean detectAudio,
        Boolean retainAudioTranscript,
        Boolean blockCopyPaste,
        Integer allowedTabSwitches,
        Integer autoReviewRiskThreshold,
        Integer autoTerminateRiskThreshold,
        Integer evidenceRetentionDays,
        Integer evidenceCapturePreSeconds,
        Integer evidenceCapturePostSeconds
) {
}
