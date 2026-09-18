package com.example.test.auth.entity;

import com.example.test.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * A single authority, named {@code resource:action} — {@code exam:publish},
 * {@code evidence:download}, {@code review:decide}.
 *
 * <p>Permissions are a table rather than a hardcoded enum because the sensitive ones here
 * (viewing a candidate's webcam recording, overriding a score) get re-scoped by policy far more
 * often than the code changes.
 */
@Entity
@Table(
        name = "permissions",
        uniqueConstraints = @UniqueConstraint(name = "uk_permissions_code", columnNames = "code"),
        indexes = @Index(name = "ix_permissions_resource", columnList = "resource"))
@Getter
@Setter
public class Permission extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String code;

    @Column(nullable = false, length = 64)
    private String resource;

    @Column(nullable = false, length = 48)
    private String action;

    @Column(length = 255)
    private String description;

    /** True for authorities the platform relies on; these cannot be deleted by an administrator. */
    @Column(name = "is_system", nullable = false)
    private boolean system = false;
}
