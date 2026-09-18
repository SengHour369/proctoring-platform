package com.example.proctoringservice.aimodel.entity;

import com.example.proctoringservice.aimodel.enums.ModelPurpose;
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

/**
 * Registry entry for one model the platform depends on — the face matcher, the object detector,
 * the risk scorer. The registry exists so detections can name their producer by key rather than
 * by a free-text string, and so a model can be swapped without touching detection code.
 */
@Entity
@Table(
        name = "ai_models",
        uniqueConstraints = @UniqueConstraint(name = "uk_ai_models_code", columnNames = "code"),
        indexes = @Index(name = "ix_ai_models_purpose", columnList = "purpose"))
@Getter
@Setter
public class AiModel extends BaseEntity {

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ModelPurpose purpose;

    @Column(length = 100)
    private String vendor;

    @Column(length = 1000)
    private String description;

    /** Where inference runs — matters for latency budgets and for data-residency questions. */
    @Column(name = "runs_on_client", nullable = false)
    private boolean runsOnClient = false;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
