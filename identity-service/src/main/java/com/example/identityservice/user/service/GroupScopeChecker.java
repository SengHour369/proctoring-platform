package com.example.identityservice.user.service;

import com.example.identityservice.usergroup.entity.GroupMembership;
import com.example.identityservice.usergroup.entity.StudentGroup;
import com.example.identityservice.usergroup.repository.GroupMembershipRepository;
import com.example.identityservice.usergroup.repository.StudentGroupRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Answers "does this teacher own/teach a group this student is currently in?" — the scope check
 * behind §2.1.4 step 2 (student profile) and the {@code activity:view_class} branch of §2.5.
 */
@Component
public class GroupScopeChecker {

    private final GroupMembershipRepository groupMembershipRepository;
    private final StudentGroupRepository studentGroupRepository;

    public GroupScopeChecker(
            GroupMembershipRepository groupMembershipRepository, StudentGroupRepository studentGroupRepository) {
        this.groupMembershipRepository = groupMembershipRepository;
        this.studentGroupRepository = studentGroupRepository;
    }

    public boolean teacherSharesGroupWithStudent(Long teacherUserId, Long studentUserId) {
        List<GroupMembership> memberships = groupMembershipRepository.findByUserIdAndActiveTrue(studentUserId);
        if (memberships.isEmpty()) {
            return false;
        }
        Set<Long> ownedGroupIds = studentGroupRepository.findByOwnerUserId(teacherUserId).stream()
                .map(StudentGroup::getId)
                .collect(Collectors.toSet());
        return memberships.stream().anyMatch(m -> ownedGroupIds.contains(m.getStudentGroupId()));
    }
}
