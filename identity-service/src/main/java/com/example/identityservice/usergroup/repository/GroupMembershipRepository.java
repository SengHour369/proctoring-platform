package com.example.identityservice.usergroup.repository;

import com.example.identityservice.usergroup.entity.GroupMembership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupMembershipRepository extends JpaRepository<GroupMembership, Long> {

    Optional<GroupMembership> findByStudentGroupIdAndUserIdAndActiveTrue(Long studentGroupId, Long userId);

    List<GroupMembership> findByUserIdAndActiveTrue(Long userId);

    List<GroupMembership> findByUserId(Long userId);
}
