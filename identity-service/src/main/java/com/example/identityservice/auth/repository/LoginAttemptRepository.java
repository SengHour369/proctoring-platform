package com.example.identityservice.auth.repository;

import com.example.identityservice.auth.entity.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;

public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {

    Page<LoginAttempt> findByUserIdAndAttemptedAtBetweenOrderByAttemptedAtDesc(
            Long userId, Instant from, Instant to, Pageable pageable);
}
