package com.example.identityservice.usergroup.repository;

import com.example.identityservice.usergroup.entity.StudentGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentGroupRepository extends JpaRepository<StudentGroup, Long> {

    Optional<StudentGroup> findByCode(String code);

    List<StudentGroup> findByOwnerUserId(Long ownerUserId);
}
