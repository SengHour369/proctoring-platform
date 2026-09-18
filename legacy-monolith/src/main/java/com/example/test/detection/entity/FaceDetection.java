package com.example.test.detection.entity;

import com.example.test.detection.enums.GazeDirection;
import com.example.test.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Face-model findings for one frame: who is in shot, how many, where they are looking, and whether
 * the face is live rather than a photo held to the camera.
 */
@Entity
@Table(
        name = "face_detections",
        uniqueConstraints = @UniqueConstraint(name = "uk_face_detections_detection", columnNames = "ai_detection_id"))
@Getter
@Setter
public class FaceDetection extends BaseEntity {

    /** The AI_DETECTIONS row this detail belongs to; exactly one detail row per detection. */
    @Column(name = "ai_detection_id", nullable = false)
    private Long aiDetectionId;

    /** 0 means the candidate left the frame; >1 means someone else is present. */
    @Column(name = "face_count", nullable = false)
    private int faceCount;

    /** Similarity against the enrolment photo, 0.0000–1.0000. */
    @Column(name = "identity_match_score", precision = 5, scale = 4)
    private BigDecimal identityMatchScore;

    /** Null when no comparison was possible (no face, or no enrolment photo). */
    @Column(name = "identity_matched")
    private Boolean identityMatched;

    @Enumerated(EnumType.STRING)
    @Column(name = "gaze_direction", length = 16)
    private GazeDirection gazeDirection = GazeDirection.UNKNOWN;

    @Column(name = "gaze_off_screen_ms")
    private Long gazeOffScreenMs;

    @Column(name = "head_yaw", precision = 6, scale = 2)
    private BigDecimal headYaw;

    @Column(name = "head_pitch", precision = 6, scale = 2)
    private BigDecimal headPitch;

    @Column(name = "head_roll", precision = 6, scale = 2)
    private BigDecimal headRoll;

    @Column(name = "eyes_closed")
    private Boolean eyesClosed;

    @Column(name = "mask_or_occlusion_detected", nullable = false)
    private boolean maskOrOcclusionDetected = false;

    /** Anti-spoofing score: low values suggest a photo, mask or replayed video. */
    @Column(name = "liveness_score", precision = 5, scale = 4)
    private BigDecimal livenessScore;

    @Column(name = "spoof_suspected", nullable = false)
    private boolean spoofSuspected = false;

    @Embedded
    private BoundingBox boundingBox;

}
