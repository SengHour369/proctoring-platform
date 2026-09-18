package com.example.identityservice.web;

import com.example.identityservice.auth.dto.ApiClientCredential;
import com.example.identityservice.auth.service.ApiClientService;
import com.example.identityservice.web.dto.AuthenticateClientRequest;
import com.example.identityservice.web.dto.AuthenticateClientResponse;
import com.example.identityservice.web.dto.RegisterClientRequest;
import com.example.identityservice.web.dto.RevokeClientRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

/** {@code X-Actor-User-Id} stands in for the authenticated caller — see {@link SecurityConfig}. */
@RestController
@RequestMapping("/api/clients")
public class ApiClientController {

    private final ApiClientService apiClientService;
    private final RequestContextResolver requestContextResolver;

    public ApiClientController(ApiClientService apiClientService, RequestContextResolver requestContextResolver) {
        this.apiClientService = apiClientService;
        this.requestContextResolver = requestContextResolver;
    }

    /** The plaintext secret in the response is shown exactly once — store it now, it is never returned again. */
    @PostMapping
    public ResponseEntity<ApiClientCredential> registerClient(
            @Valid @RequestBody RegisterClientRequest request,
            @RequestHeader("X-Actor-User-Id") Long actorUserId) {
        ApiClientCredential credential = apiClientService.registerClient(
                request.name(), request.allowedScopes(), actorUserId, request.rateLimitPerMinute(), request.allowedIpRanges());
        return ResponseEntity.ok(credential);
    }

    @PostMapping("/{clientId}/rotate-secret")
    public ResponseEntity<ApiClientCredential> rotateSecret(@PathVariable String clientId) {
        return ResponseEntity.ok(apiClientService.rotateSecret(clientId));
    }

    @PostMapping("/{clientId}/revoke")
    public ResponseEntity<Void> revoke(@PathVariable String clientId, @Valid @RequestBody RevokeClientRequest request) {
        apiClientService.revoke(clientId, request.reason());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticateClientResponse> authenticate(
            @Valid @RequestBody AuthenticateClientRequest request, HttpServletRequest httpRequest) {
        String requestIp = requestContextResolver.clientIp(httpRequest);
        return apiClientService.authenticate(request.clientId(), request.secret(), requestIp)
                .map(scopes -> ResponseEntity.ok(new AuthenticateClientResponse(request.clientId(), Arrays.asList(scopes))))
                .orElseGet(() -> ResponseEntity.status(401).build());
    }
}
