package com.example.test.exam.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import com.example.test.exam.enums.ProctoringMode;
import lombok.Getter;
import lombok.Setter;

/**
 * Supervision rules an exam enforces. Modelled as a value object embedded in EXAMS rather than a
 * table of its own: a policy has no identity or lifecycle apart from the exam that declares it.
 */
@Embeddable
@Getter
@Setter
public class ProctoringPolicy {

    @Enumerated(EnumType.STRING)
    @Column(name = "proctoring_mode", nullable = false, length = 32)
    private ProctoringMode mode = ProctoringMode.NONE;

    @Column(name = "require_webcam", nullable = false)
    private boolean requireWebcam = false;

    @Column(name = "require_screen_share", nullable = false)
    private boolean requireScreenShare = false;

    @Column(name = "require_microphone", nullable = false)
    private boolean requireMicrophone = false;

    @Column(name = "require_identity_check", nullable = false)
    private boolean requireIdentityCheck = false;

    @Column(name = "require_environment_scan", nullable = false)
    private boolean requireEnvironmentScan = false;

    @Column(name = "force_fullscreen", nullable = false)
    private boolean forceFullscreen = false;

    @Column(name = "monitor_screen", nullable = false)
    private boolean monitorScreen = false;

    @Column(name = "detect_tab_switch", nullable = false)
    private boolean detectTabSwitch = false;

    /*
     * Which AI detectors run for this exam. Separate from the require* flags above: requiring a
     * webcam is about admission to the sitting, enabling face detection is about what is done with
     * the stream afterwards. An exam can demand a camera purely for recorded review, and a low
     * stakes exam can run with a camera and no inference at all — so the two must be independent.
     *
     * Each flag also has a cost: every detector enabled here is a model invocation per frame per
     * candidate, and rows in AI_DETECTIONS for the lifetime of the retention policy.
     */

    @Column(name = "detect_face", nullable = false)
    private boolean detectFace = false;

    @Column(name = "detect_multiple_faces", nullable = false)
    private boolean detectMultipleFaces = false;

    /** Head pose and gaze estimation — the "looking away" signals. */
    @Column(name = "detect_gaze", nullable = false)
    private boolean detectGaze = false;

    /** Prohibited-object detection: phone, book, second laptop, additional person. */
    @Column(name = "detect_objects", nullable = false)
    private boolean detectObjects = false;

    @Column(name = "detect_audio", nullable = false)
    private boolean detectAudio = false;

    /** Retain a short excerpt of transcribed speech on an audio detection. Off by default. */
    @Column(name = "retain_audio_transcript", nullable = false)
    private boolean retainAudioTranscript = false;

    @Column(name = "block_copy_paste", nullable = false)
    private boolean blockCopyPaste = false;

    /** Tab switches tolerated before the platform escalates. Null means unlimited. */
    @Column(name = "allowed_tab_switches")
    private Integer allowedTabSwitches;

    /** Risk score at or above which the attempt is auto-flagged for human review. */
    @Column(name = "auto_review_risk_threshold")
    private Integer autoReviewRiskThreshold;

    /** Risk score at or above which the attempt is terminated without a human in the loop. */
    @Column(name = "auto_terminate_risk_threshold")
    private Integer autoTerminateRiskThreshold;

    /** Days evidence is retained before purge, per data-protection policy. */
    @Column(name = "evidence_retention_days")
    private Integer evidenceRetentionDays;
}
