package com.example.identityservice.auth.repository;

import com.example.identityservice.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByExternalRef(String externalRef);

    Optional<User> findByPublicId(UUID publicId);
}
