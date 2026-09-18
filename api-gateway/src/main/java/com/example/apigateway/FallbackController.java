package com.example.apigateway;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Reached when a route's circuit breaker is open (the target service is down or timing
 * out) instead of the caller getting a raw connection-refused error.
 */
@RestController
public class FallbackController {

    @RequestMapping("/fallback/{service}")
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Mono<Map<String, Object>> fallback(@PathVariable String service) {
        return Mono.just(Map.of(
                "status", HttpStatus.SERVICE_UNAVAILABLE.value(),
                "service", service,
                "message", service + " is temporarily unavailable — please retry shortly."
        ));
    }
}
