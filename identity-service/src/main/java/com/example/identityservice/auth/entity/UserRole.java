package com.example.identityservice.auth.entity;

import com.example.identityservice.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Grant of one role to one user, resolving the many-to-many between USERS and ROLES. The grant is
 * an entity in its own right rather than a bare pair of ids: it carries who granted it, when, and
 * when it lapses — which matters when the role in question is REVIEWER.
 */
@Entity
@Table(
        name = "user_role",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_role", columnNames = {"user_id", "role_id"}))
@Getter
@Setter
public class UserRole extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(name = "granted_by_user_id")
    private Long grantedByUserId;

    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt = Instant.now();

    @Column(name = "expires_at")
    private Instant expiresAt;
}
