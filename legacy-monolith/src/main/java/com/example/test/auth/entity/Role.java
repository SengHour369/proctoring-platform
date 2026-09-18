package com.example.test.auth.entity;

import com.example.test.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * Named permission bundle — STUDENT, TEACHER, REVIEWER, ADMIN. Kept as a table rather than an
 * enum so an operator can add a role (an external invigilator, a read-only auditor) without a
 * redeploy, and so {@link RolePermission} can re-scope an existing one.
 */
@Entity
@Table(
        name = "roles",
        uniqueConstraints = @UniqueConstraint(name = "uk_roles_code", columnNames = "code"))
@Getter
@Setter
public class Role extends BaseEntity {

    @Column(nullable = false, length = 48)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(name = "is_system", nullable = false)
    private boolean system = false;
}
