package com.example.proctoringservice.detection.entity;

import com.example.proctoringservice.detection.enums.AudioEventType;
import com.example.proctoringservice.common.entity.BaseEntity;
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
import java.time.Instant;

/**
 * Audio-model findings over a listening window: speech while alone in the room, a second voice,
 * sustained background noise, or the microphone going silent when it should not.
 *
 * <p>The fourth branch of the detection hierarchy, sharing AI_DETECTIONS with the vision models.
 * {@code speakerCount} is what separates a candidate muttering to themselves from a candidate
 * being coached; {@code transcriptExcerpt} is deliberately short and optional, since transcribing
 * a whole exam room raises a privacy cost the risk score rarely needs.
 */
@Entity
@Table(
        name = "audio_detections",
        uniqueConstraints = @UniqueConstraint(name = "uk_audio_detections_detection", columnNames = "ai_detection_id"),
        indexes = @Index(name = "ix_audio_detections_type", columnList = "audio_event_type"))
@Getter
@Setter
public class AudioDetection extends BaseEntity {

    /** The AI_DETECTIONS row this detail belongs to; exactly one detail row per detection. */
    @Column(name = "ai_detection_id", nullable = false)
    private Long aiDetectionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "audio_event_type", nullable = false, length = 32)
    private AudioEventType audioEventType;

    @Column(name = "window_start_at", nullable = false)
    private Instant windowStartAt;

    @Column(name = "window_end_at", nullable = false)
    private Instant windowEndAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    /** Distinct voices heard. Greater than one is a strong impersonation or coaching signal. */
    @Column(name = "speaker_count")
    private Integer speakerCount;

    /** True when a voice was heard that does not match the candidate's enrolled voiceprint. */
    @Column(name = "unknown_speaker", nullable = false)
    private boolean unknownSpeaker = false;

    @Column(name = "peak_db", precision = 6, scale = 2)
    private BigDecimal peakDb;

    @Column(name = "average_db", precision = 6, scale = 2)
    private BigDecimal averageDb;

    @Column(name = "signal_to_noise_ratio", precision = 6, scale = 2)
    private BigDecimal signalToNoiseRatio;

    @Column(name = "speech_ratio", precision = 5, scale = 4)
    private BigDecimal speechRatio;

    @Column(name = "language_code", length = 8)
    private String languageCode;

    /** Short excerpt retained only when the exam policy allows it. */
    @Column(name = "transcript_excerpt", length = 1000)
    private String transcriptExcerpt;

}
