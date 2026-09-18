package com.example.identityservice.auth.repository;

import com.example.identityservice.auth.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    List<UserRole> findByUserId(Long userId);

    Optional<UserRole> findByUserIdAndRoleId(Long userId, Long roleId);

    List<UserRole> findByRoleId(Long roleId);
}
