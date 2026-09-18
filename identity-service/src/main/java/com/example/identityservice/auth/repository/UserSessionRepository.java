package com.example.identityservice.auth.repository;

import com.example.identityservice.auth.entity.UserSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    Optional<UserSession> findByRefreshTokenHash(String refreshTokenHash);

    List<UserSession> findByUserIdAndRevokedAtIsNull(Long userId);

    Page<UserSession> findByUserIdAndIssuedAtBetweenOrderByIssuedAtDesc(
            Long userId, Instant from, Instant to, Pageable pageable);
}
