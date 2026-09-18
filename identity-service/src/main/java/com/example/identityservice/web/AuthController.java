package com.example.identityservice.web;

import com.example.identityservice.auth.dto.LoginResult;
import com.example.identityservice.auth.dto.RequestContext;
import com.example.identityservice.auth.service.AuthService;
import com.example.identityservice.web.dto.EnrollMfaRequest;
import com.example.identityservice.web.dto.ForgotPasswordRequest;
import com.example.identityservice.web.dto.LoginRequest;
import com.example.identityservice.web.dto.LoginResponse;
import com.example.identityservice.web.dto.RefreshTokenRequest;
import com.example.identityservice.web.dto.ResetPasswordRequest;
import com.example.identityservice.web.dto.VerifyEmailRequest;
import com.example.identityservice.web.dto.VerifyMfaRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RequestContextResolver requestContextResolver;

    public AuthController(AuthService authService, RequestContextResolver requestContextResolver) {
        this.authService = authService;
        this.requestContextResolver = requestContextResolver;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        RequestContext ctx = requestContextResolver.resolve(httpRequest);
        LoginResult result = authService.login(request.email(), request.password(), ctx);
        return ResponseEntity.ok(LoginResponse.from(result));
    }

    @PostMapping("/mfa/verify")
    public ResponseEntity<LoginResponse> verifyMfa(@Valid @RequestBody VerifyMfaRequest request, HttpServletRequest httpRequest) {
        RequestContext ctx = requestContextResolver.resolve(httpRequest);
        LoginResult result = authService.verifyMfaCode(request.userId(), request.code(), ctx);
        return ResponseEntity.ok(LoginResponse.from(result));
    }

    @PostMapping("/logout/{sessionId}")
    public ResponseEntity<Void> logout(@PathVariable Long sessionId) {
        authService.logout(sessionId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        LoginResult result = authService.refreshToken(request.refreshToken());
        return ResponseEntity.ok(LoginResponse.from(result));
    }

    /** Always 200 with the same message, regardless of whether the email matches an account. */
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request, HttpServletRequest httpRequest) {
        RequestContext ctx = requestContextResolver.resolve(httpRequest);
        authService.forgotPassword(request.email(), ctx);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request, HttpServletRequest httpRequest) {
        String requestIp = requestContextResolver.clientIp(httpRequest);
        authService.resetPassword(request.token(), request.newPassword(), requestIp);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest request, HttpServletRequest httpRequest) {
        String requestIp = requestContextResolver.clientIp(httpRequest);
        authService.verifyEmail(request.token(), requestIp);
        return ResponseEntity.ok().build();
    }

    /** Returns the plaintext TOTP seed once, for QR setup; null body for SMS/EMAIL. */
    @PostMapping("/mfa/enroll")
    public ResponseEntity<String> enrollMfa(@Valid @RequestBody EnrollMfaRequest request) {
        String seed = authService.enrollMfa(request.userId(), request.method());
        return ResponseEntity.ok(seed);
    }
}
