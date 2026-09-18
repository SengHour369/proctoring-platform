package com.example.identityservice.auth.repository;

import com.example.identityservice.auth.entity.SecurityToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SecurityTokenRepository extends JpaRepository<SecurityToken, Long> {

    Optional<SecurityToken> findByTokenHash(String tokenHash);
}
