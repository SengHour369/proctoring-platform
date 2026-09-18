package com.example.identityservice.usergroup.entity;

import com.example.identityservice.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Membership of one student in one group. {@code leftAt} rather than deletion: an exam assigned to
 * a class must stay explicable a year later, when the class roster has moved on.
 */
@Entity
@Table(
        name = "group_memberships",
        uniqueConstraints = @UniqueConstraint(name = "uk_group_memberships", columnNames = {"student_group_id", "user_id"}),
        indexes = @Index(name = "ix_group_memberships_user", columnList = "user_id"))
@Getter
@Setter
public class GroupMembership extends BaseEntity {

    @Column(name = "student_group_id", nullable = false)
    private Long studentGroupId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "added_by_user_id")
    private Long addedByUserId;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt = Instant.now();

    @Column(name = "left_at")
    private Instant leftAt;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
