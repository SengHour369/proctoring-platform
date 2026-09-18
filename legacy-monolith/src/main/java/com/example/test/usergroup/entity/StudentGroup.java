package com.example.test.usergroup.entity;

import com.example.test.common.entity.BaseEntity;
import com.example.test.usergroup.enums.GroupType;
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
 * A class, cohort or ad-hoc group of students, so an exam can be assigned to forty people in one
 * action. Self-referencing so a programme can contain classes.
 */
@Entity
@Table(
        name = "student_groups",
        uniqueConstraints = @UniqueConstraint(name = "uk_student_groups_code", columnNames = "code"),
        indexes = @Index(name = "ix_student_groups_type", columnList = "group_type"))
@Getter
@Setter
public class StudentGroup extends BaseEntity {

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "group_type", nullable = false, length = 24)
    private GroupType groupType = GroupType.CLASS;

    @Column(name = "parent_group_id")
    private Long parentGroupId;

    /** Teacher or coordinator responsible for the group. */
    @Column(name = "owner_user_id")
    private Long ownerUserId;

    @Column(name = "academic_term", length = 64)
    private String academicTerm;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
