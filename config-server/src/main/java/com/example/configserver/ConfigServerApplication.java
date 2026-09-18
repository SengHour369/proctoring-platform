package com.example.configserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * Centralised configuration for every service. Backed by the local {@code config-repo}
 * folder (native profile) rather than a git remote, so this runs standalone with no
 * external dependency — point {@code spring.cloud.config.server.git.uri} at a real repo
 * instead once one exists.
 */
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
