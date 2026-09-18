package com.example.identityservice.auth.repository;

import com.example.identityservice.auth.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

    List<RolePermission> findByRoleId(Long roleId);

    Optional<RolePermission> findByRoleIdAndPermissionId(Long roleId, Long permissionId);

    void deleteByRoleId(Long roleId);
}
