package com.example.identityservice.auth.repository;

import com.example.identityservice.auth.entity.ApiClient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ApiClientRepository extends JpaRepository<ApiClient, Long> {

    Optional<ApiClient> findByClientId(String clientId);
}
