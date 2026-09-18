package com.example.test.auth.entity;

import com.example.test.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Grant of one {@link Permission} to one {@link Role}, with its own audit trail. Access control is
 * evaluated as user → roles → permissions, so this table is the whole of RBAC.
 */
@Entity
@Table(
        name = "role_permission",
        uniqueConstraints = @UniqueConstraint(name = "uk_role_permission", columnNames = {"role_id", "permission_id"}))
@Getter
@Setter
public class RolePermission extends BaseEntity {

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(name = "permission_id", nullable = false)
    private Long permissionId;

    @Column(name = "granted_by_user_id")
    private Long grantedByUserId;

    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt = Instant.now();
}
