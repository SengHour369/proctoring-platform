# Auth Module — Full Source Code

Spring Boot auth module with email verification, JWT authentication, refresh tokens, and password reset.

---

## All Endpoints

| Method | Path | Auth Required | Description |
|--------|------|---------------|-------------|
| POST | `/api/auth/register` | No | Register new user |
| GET | `/api/auth/verify` | No | Verify email with token |
| POST | `/api/auth/resend` | No | Resend verification email |
| POST | `/api/auth/login/email` | No | Login with email + password |
| POST | `/api/auth/login/username` | No | Login with username + password |
| POST | `/api/auth/refresh` | No | Refresh access token |
| POST | `/api/auth/logout` | No | Logout (delete refresh token) |
| POST | `/api/auth/forgot-password` | No | Request password reset email |
| POST | `/api/auth/reset-password` | No | Reset password with token |

---

## API Usage Examples

### 1. Register

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "senghour",
    "email": "senghour@example.com",
    "password": "secret123"
  }'
```

**Response `201 Created`:**
```json
{
  "success": true,
  "message": "User registered successfully",
  "status": 201,
  "timestamp": "2026-05-28T10:00:00",
  "data": {
    "success": true,
    "message": "Registration successful. Please check your email to verify your account."
  }
}
```

---

### 2. Verify Email

```bash
curl -X GET "http://localhost:8080/api/auth/verify?token=550e8400-e29b-41d4-a716-446655440000"
```

**Response `200 OK`:**
```json
{
  "success": true,
  "message": "Email verified",
  "status": 200,
  "timestamp": "2026-05-28T10:05:00",
  "data": {
    "success": true,
    "message": "Email verified successfully. You can now log in."
  }
}
```

**Response `410 Gone` (token expired):**
```json
{
  "success": false,
  "message": "Verification token has expired. Please request a new one.",
  "status": 410,
  "timestamp": "2026-05-28T10:05:00",
  "data": null
}
```

---

### 3. Resend Verification Email

```bash
curl -X POST "http://localhost:8080/api/auth/resend?email=senghour@example.com"
```

**Response `200 OK`:**
```json
{
  "success": true,
  "message": "Verification email resent",
  "status": 200,
  "timestamp": "2026-05-28T10:10:00",
  "data": {
    "success": true,
    "message": "Verification email resent. Please check your inbox."
  }
}
```

---

### 4. Login by Email

```bash
curl -X POST http://localhost:8080/api/auth/login/email \
  -H "Content-Type: application/json" \
  -d '{
    "email": "senghour@example.com",
    "password": "secret123"
  }'
```

**Response `200 OK`:**
```json
{
  "success": true,
  "message": "Login successful",
  "status": 200,
  "timestamp": "2026-05-28T10:15:00",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJzZW5naG91ckBleGFtcGxlLmNvbSJ9.xxx",
    "refreshToken": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "tokenType": "Bearer",
    "email": "senghour@example.com",
    "displayName": "senghour",
    "role": "USER"
  }
}
```

---

### 5. Login by Username

```bash
curl -X POST http://localhost:8080/api/auth/login/username \
  -H "Content-Type: application/json" \
  -d '{
    "username": "senghour",
    "password": "secret123"
  }'
```

**Response `200 OK`:** Same shape as login by email.

---

### 6. Call a Protected Endpoint

Use the `accessToken` from login in the `Authorization` header:

```bash
curl -X GET http://localhost:8080/api/some-protected-resource \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJzZW5naG91ckBleGFtcGxlLmNvbSJ9.xxx"
```

**Response `401 Unauthorized`** if token is missing, invalid, or expired.

---

### 7. Refresh Access Token

When the access token expires (after 24 hours), use the refresh token to get a new one:

```bash
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
  }'
```

**Response `200 OK`:**
```json
{
  "success": true,
  "message": "Token refreshed",
  "status": 200,
  "timestamp": "2026-05-28T11:00:00",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9.new-token.xxx",
    "refreshToken": "z9y8x7w6-v5u4-3210-tsr-qponmlkjih98",
    "tokenType": "Bearer",
    "email": "senghour@example.com",
    "displayName": "senghour",
    "role": "USER"
  }
}
```

> The old refresh token is deleted and a new one is issued (token rotation).

**Response `410 Gone`** if refresh token is expired — user must log in again.

---

### 8. Logout

```bash
curl -X POST http://localhost:8080/api/auth/logout \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
  }'
```

**Response `200 OK`:**
```json
{
  "success": true,
  "message": "Logged out successfully",
  "status": 200,
  "timestamp": "2026-05-28T11:30:00",
  "data": {
    "success": true,
    "message": "Logged out successfully."
  }
}
```

> Access tokens remain valid until they expire — only the refresh token is invalidated.

---

### 9. Forgot Password

```bash
curl -X POST http://localhost:8080/api/auth/forgot-password \
  -H "Content-Type: application/json" \
  -d '{
    "email": "senghour@example.com"
  }'
```

**Response `200 OK`** (always, even if email is not registered):
```json
{
  "success": true,
  "message": "Password reset email sent",
  "status": 200,
  "timestamp": "2026-05-28T12:00:00",
  "data": {
    "success": true,
    "message": "If that email is registered, a password reset link has been sent."
  }
}
```

> Always returns 200 regardless of whether the email exists — prevents email enumeration.

---

### 10. Reset Password

After clicking the link in the reset email (`/api/auth/reset-password?token=xxx`), submit the new password:

```bash
curl -X POST http://localhost:8080/api/auth/reset-password \
  -H "Content-Type: application/json" \
  -d '{
    "token": "550e8400-e29b-41d4-a716-446655440000",
    "newPassword": "newSecret456"
  }'
```

**Response `200 OK`:**
```json
{
  "success": true,
  "message": "Password reset successfully",
  "status": 200,
  "timestamp": "2026-05-28T12:05:00",
  "data": {
    "success": true,
    "message": "Password reset successfully. You can now log in with your new password."
  }
}
```

**Response `400 Bad Request`** if token was already used:
```json
{
  "success": false,
  "message": "Password reset token has already been used.",
  "status": 400,
  "timestamp": "2026-05-28T12:05:00",
  "data": null
}
```

> All existing refresh tokens (active sessions) are invalidated after a password reset.

---

## Feature Processes

### 1. Register + Email Verification

```
POST /api/auth/register
  │
  ├─ Email already exists? ──────────────────► 409 EmailAlreadyExistsException
  │
  ├─ Save User (enabled=false, role=USER)
  ├─ Generate UUID token → save VerificationToken (expires +24h)
  └─ Send verification email async ──────────► Gmail SMTP
                                               Link: /api/auth/verify?token=xxx

GET /api/auth/verify?token=xxx
  │
  ├─ Token not found? ───────────────────────► 400 InvalidTokenException
  ├─ Token expired? → delete token ──────────► 410 TokenExpiredException
  ├─ User already enabled? ──────────────────► 409 UserAlreadyVerifiedException
  │
  ├─ Set user.enabled = true
  └─ Delete token ────────────────────────────► 200 OK

POST /api/auth/resend?email=xxx
  │
  ├─ User not found? ────────────────────────► 400 InvalidTokenException
  ├─ User already enabled? ──────────────────► 409 UserAlreadyVerifiedException
  │
  ├─ Delete old token
  ├─ Generate new UUID token (expires +24h)
  └─ Send verification email async ──────────► 200 OK
```

---

### 2. Login + Token Issuance

```
POST /api/auth/login/email   { email, password }
POST /api/auth/login/username  { username, password }
  │
  ├─ AuthenticationManager.authenticate() ──► 401 if credentials invalid
  │                                           401 if user.enabled = false
  │
  ├─ Generate JWT access token (expires +24h)
  ├─ Delete all previous refresh tokens for user
  ├─ Generate UUID refresh token → save RefreshToken (expires +refresh-expiration)
  └─ Return JwtAuthResponse ─────────────────► 200 OK
       { accessToken, refreshToken, tokenType, email, displayName, role }
```

---

### 3. Refresh Access Token

```
POST /api/auth/refresh   { refreshToken }
  │
  ├─ Token not found? ───────────────────────► 400 InvalidTokenException
  ├─ Token expired? → delete token ──────────► 410 TokenExpiredException
  │
  ├─ Generate new JWT access token
  ├─ Delete old refresh token
  ├─ Generate new refresh token (rotation)
  └─ Return JwtAuthResponse ─────────────────► 200 OK
```

---

### 4. Logout

```
POST /api/auth/logout   { refreshToken }
  │
  ├─ Find refresh token → delete it (silently skip if not found)
  └─ Return success ─────────────────────────► 200 OK
```

---

### 5. Forgot Password + Reset

```
POST /api/auth/forgot-password   { email }
  │
  ├─ User not found? → silently do nothing (prevents email enumeration)
  │
  ├─ Delete existing password reset tokens for user
  ├─ Generate UUID token → save PasswordResetToken (expires +1h)
  └─ Send password reset email async ────────► 200 OK (always)
       Link: /api/auth/reset-password?token=xxx

POST /api/auth/reset-password   { token, newPassword }
  │
  ├─ Token not found? ───────────────────────► 400 InvalidTokenException
  ├─ Token expired? → delete token ──────────► 410 TokenExpiredException
  ├─ Token already used? ────────────────────► 400 InvalidTokenException
  │
  ├─ BCrypt-encode new password → save user
  ├─ Mark reset token as used (used=true)
  ├─ Delete ALL refresh tokens for user (force re-login on all sessions)
  └─ Return success ─────────────────────────► 200 OK
```

---

## Error Responses

| Exception | HTTP Status | When |
|-----------|-------------|------|
| `EmailAlreadyExistsException` | 409 Conflict | Email already registered |
| `InvalidTokenException` | 400 Bad Request | Token not found, wrong purpose, or already used |
| `TokenExpiredException` | 410 Gone | Token past `expiresAt` |
| `UserAlreadyVerifiedException` | 409 Conflict | Account already verified |
| `MethodArgumentNotValidException` | 400 Bad Request | Validation failure |
| `Exception` | 500 Internal Server Error | Unexpected error |

---

## build.gradle

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '4.0.6'
    id 'io.spring.dependency-management' version '1.1.7'
}

group = 'com.example'
version = '0.0.1-SNAPSHOT'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-mail'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'io.jsonwebtoken:jjwt-api:0.12.6'
    runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.6'
    runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.6'
    runtimeOnly 'org.postgresql:postgresql'
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.security:spring-security-test'
    testCompileOnly 'org.projectlombok:lombok'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
    testAnnotationProcessor 'org.projectlombok:lombok'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

---

## application.yml

```yaml
spring:
  application:
    name: test

  datasource:
    url: jdbc:postgresql://localhost:5434/postgres
    username: senghour
    password: password
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true

  mail:
    host: smtp.gmail.com
    port: 587
    username: your_email@gmail.com
    password: your_app_password
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
            required: true

app:
  base-url: http://localhost:8080
  jwt:
    # Base64-encoded 256-bit secret — replace with a strong random value in production
    secret: dGhpcy1pcy1hLXZlcnktc2VjdXJlLXNlY3JldC1rZXktZm9yLWp3dC10b2tlbg==
    expiration: 86400000
    # refresh-expiration is required — add it (e.g. 604800000 for 7 days)
    # refresh-expiration: 604800000
```

---

## TestApplication.java

```java
package com.example.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class TestApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }
}
```

---

## DTO/APIResponse.java

```java
package com.example.test.DTO;

import java.time.LocalDateTime;

public class APIResponse<T> {

    private boolean success;
    private String message;
    private int status;
    private LocalDateTime timestamp;
    private T data;

    public APIResponse() {
    }

    public APIResponse(boolean success, String message, int status, T data) {
        this.success = success;
        this.message = message;
        this.status = status;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }

    public static <T> APIResponse<T> success(String message, int status, T data) {
        return new APIResponse<>(true, message, status, data);
    }

    public static <T> APIResponse<T> error(String message, int status) {
        return new APIResponse<>(false, message, status, null);
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public int getStatus() { return status; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public T getData() { return data; }
}
```

---

## Exception/GlobalExceptionHandler.java

```java
package com.example.test.Exception;

import com.example.test.DTO.APIResponse;
import com.example.test.auth.exception.EmailAlreadyExistsException;
import com.example.test.auth.exception.InvalidTokenException;
import com.example.test.auth.exception.TokenExpiredException;
import com.example.test.auth.exception.UserAlreadyVerifiedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<APIResponse<Void>> handleInvalidToken(InvalidTokenException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(APIResponse.error(ex.getMessage(), HttpStatus.BAD_REQUEST.value()));
    }

    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<APIResponse<Void>> handleTokenExpired(TokenExpiredException ex) {
        return ResponseEntity
                .status(HttpStatus.GONE)
                .body(APIResponse.error(ex.getMessage(), HttpStatus.GONE.value()));
    }

    @ExceptionHandler(UserAlreadyVerifiedException.class)
    public ResponseEntity<APIResponse<Void>> handleAlreadyVerified(UserAlreadyVerifiedException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(APIResponse.error(ex.getMessage(), HttpStatus.CONFLICT.value()));
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<APIResponse<Void>> handleEmailExists(EmailAlreadyExistsException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(APIResponse.error(ex.getMessage(), HttpStatus.CONFLICT.value()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<APIResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(APIResponse.error(errors, HttpStatus.BAD_REQUEST.value()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<APIResponse<Void>> handleGeneral(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(APIResponse.error("An unexpected error occurred.", HttpStatus.INTERNAL_SERVER_ERROR.value()));
    }
}
```

---

## auth/config/ApplicationConfig.java

```java
package com.example.test.auth.config;

import com.example.test.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class ApplicationConfig {

    private final UserRepository userRepository;

    @Bean
    public UserDetailsService userDetailsService() {
        return identifier -> userRepository.findByEmail(identifier)
                .or(() -> userRepository.findByUsername(identifier))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + identifier));
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService());
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

---

## auth/config/SecurityConfig.java

```java
package com.example.test.auth.config;

import com.example.test.auth.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

---

## auth/controller/AuthController.java

```java
package com.example.test.auth.controller;

import com.example.test.DTO.APIResponse;
import com.example.test.auth.dto.request.ForgotPasswordRequest;
import com.example.test.auth.dto.request.LoginRequest;
import com.example.test.auth.dto.request.RefreshTokenRequest;
import com.example.test.auth.dto.request.RegisterRequest;
import com.example.test.auth.dto.request.ResetPasswordRequest;
import com.example.test.auth.dto.request.UsernameLoginRequest;
import com.example.test.auth.dto.response.AuthResponse;
import com.example.test.auth.dto.response.JwtAuthResponse;
import com.example.test.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<APIResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(APIResponse.success("User registered successfully", HttpStatus.CREATED.value(), response));
    }

    @GetMapping("/verify")
    public ResponseEntity<APIResponse<AuthResponse>> verify(@RequestParam String token) {
        AuthResponse response = authService.verifyEmail(token);
        return ResponseEntity.ok(APIResponse.success("Email verified", HttpStatus.OK.value(), response));
    }

    @PostMapping("/resend")
    public ResponseEntity<APIResponse<AuthResponse>> resend(@RequestParam String email) {
        AuthResponse response = authService.resendVerification(email);
        return ResponseEntity.ok(APIResponse.success("Verification email resent", HttpStatus.OK.value(), response));
    }

    @PostMapping("/login/email")
    public ResponseEntity<APIResponse<JwtAuthResponse>> loginByEmail(@Valid @RequestBody LoginRequest request) {
        JwtAuthResponse response = authService.loginByEmail(request);
        return ResponseEntity.ok(APIResponse.success("Login successful", HttpStatus.OK.value(), response));
    }

    @PostMapping("/login/username")
    public ResponseEntity<APIResponse<JwtAuthResponse>> loginByUsername(@Valid @RequestBody UsernameLoginRequest request) {
        JwtAuthResponse response = authService.loginByUsername(request);
        return ResponseEntity.ok(APIResponse.success("Login successful", HttpStatus.OK.value(), response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<APIResponse<JwtAuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        JwtAuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(APIResponse.success("Token refreshed", HttpStatus.OK.value(), response));
    }

    @PostMapping("/logout")
    public ResponseEntity<APIResponse<AuthResponse>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.logout(request);
        return ResponseEntity.ok(APIResponse.success("Logged out successfully", HttpStatus.OK.value(), response));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<APIResponse<AuthResponse>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        AuthResponse response = authService.forgotPassword(request);
        return ResponseEntity.ok(APIResponse.success("Password reset email sent", HttpStatus.OK.value(), response));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<APIResponse<AuthResponse>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        AuthResponse response = authService.resetPassword(request);
        return ResponseEntity.ok(APIResponse.success("Password reset successfully", HttpStatus.OK.value(), response));
    }
}
```

---

## auth/dto/request/RegisterRequest.java

```java
package com.example.test.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    @Size(max = 50, message = "Username must not exceed 50 characters")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    private String password;
}
```

---

## auth/dto/request/LoginRequest.java

```java
package com.example.test.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;
}
```

---

## auth/dto/request/UsernameLoginRequest.java

```java
package com.example.test.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UsernameLoginRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;
}
```

---

## auth/dto/request/RefreshTokenRequest.java

```java
package com.example.test.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefreshTokenRequest {

    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}
```

---

## auth/dto/request/ForgotPasswordRequest.java

```java
package com.example.test.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ForgotPasswordRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;
}
```

---

## auth/dto/request/ResetPasswordRequest.java

```java
package com.example.test.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequest {

    @NotBlank(message = "Token is required")
    private String token;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    private String newPassword;
}
```

---

## auth/dto/response/AuthResponse.java

```java
package com.example.test.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {

    private boolean success;
    private String message;
}
```

---

## auth/dto/response/JwtAuthResponse.java

```java
package com.example.test.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class JwtAuthResponse {

    private String accessToken;
    private String refreshToken;
    @Builder.Default
    private String tokenType = "Bearer";
    private String email;
    private String displayName;
    private String role;
}
```

---

## auth/entity/Role.java

```java
package com.example.test.auth.entity;

public enum Role {
    USER, ADMIN
}
```

---

## auth/entity/User.java

```java
package com.example.test.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Getter(AccessLevel.NONE)
    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.USER;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** Returns the display username (not the security principal). */
    public String getDisplayName() {
        return username;
    }

    // UserDetails — email is the security principal stored in JWT
    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
}
```

---

## auth/entity/VerificationToken.java

```java
package com.example.test.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "verification_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String token;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
```

---

## auth/entity/RefreshToken.java

```java
package com.example.test.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
```

---

## auth/entity/PasswordResetToken.java

```java
package com.example.test.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "password_reset_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Builder.Default
    @Column(nullable = false)
    private boolean used = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
```

---

## auth/exception/EmailAlreadyExistsException.java

```java
package com.example.test.auth.exception;

public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String message) {
        super(message);
    }
}
```

---

## auth/exception/InvalidTokenException.java

```java
package com.example.test.auth.exception;

public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(String message) {
        super(message);
    }
}
```

---

## auth/exception/TokenExpiredException.java

```java
package com.example.test.auth.exception;

public class TokenExpiredException extends RuntimeException {

    public TokenExpiredException(String message) {
        super(message);
    }
}
```

---

## auth/exception/UserAlreadyVerifiedException.java

```java
package com.example.test.auth.exception;

public class UserAlreadyVerifiedException extends RuntimeException {

    public UserAlreadyVerifiedException(String message) {
        super(message);
    }
}
```

---

## auth/repository/UserRepository.java

```java
package com.example.test.auth.repository;

import com.example.test.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);
}
```

---

## auth/repository/VerificationTokenRepository.java

```java
package com.example.test.auth.repository;

import com.example.test.auth.entity.User;
import com.example.test.auth.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {

    Optional<VerificationToken> findByToken(String token);

    void deleteByUser(User user);
}
```

---

## auth/repository/RefreshTokenRepository.java

```java
package com.example.test.auth.repository;

import com.example.test.auth.entity.RefreshToken;
import com.example.test.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByToken(String token);

    void deleteByUser(User user);
}
```

---

## auth/repository/PasswordResetTokenRepository.java

```java
package com.example.test.auth.repository;

import com.example.test.auth.entity.PasswordResetToken;
import com.example.test.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByToken(String token);

    void deleteByUser(User user);
}
```

---

## auth/security/JwtService.java

```java
package com.example.test.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration}")
    private long jwtExpiration;

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String subject = extractUsername(token);
        return subject.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
```

---

## auth/security/JwtAuthenticationFilter.java

```java
package com.example.test.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);
        final String userEmail = jwtService.extractUsername(jwt);

        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

            if (jwtService.isTokenValid(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}
```

---

## auth/service/AuthService.java

```java
package com.example.test.auth.service;

import com.example.test.auth.dto.request.*;
import com.example.test.auth.dto.response.AuthResponse;
import com.example.test.auth.dto.response.JwtAuthResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse verifyEmail(String token);

    AuthResponse resendVerification(String email);

    JwtAuthResponse loginByEmail(LoginRequest request);

    JwtAuthResponse loginByUsername(UsernameLoginRequest request);

    JwtAuthResponse refreshToken(RefreshTokenRequest request);

    AuthResponse logout(RefreshTokenRequest request);

    AuthResponse forgotPassword(ForgotPasswordRequest request);

    AuthResponse resetPassword(ResetPasswordRequest request);
}
```

---

## auth/service/EmailService.java

```java
package com.example.test.auth.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendVerificationEmail(String toEmail, String token) {
        try {
            String verificationLink = baseUrl + "/api/auth/verify?token=" + token;

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Verify your email address");
            helper.setText(buildEmailBody(verificationLink), true);

            mailSender.send(message);
            log.info("Verification email sent to: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send verification email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String token) {
        try {
            String resetLink = baseUrl + "/api/auth/reset-password?token=" + token;

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Reset your password");
            helper.setText(buildPasswordResetBody(resetLink), true);

            mailSender.send(message);
            log.info("Password reset email sent to: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage());
        }
    }

    private String buildEmailBody(String verificationLink) {
        return """
                <html>
                  <body style="font-family: Arial, sans-serif; padding: 20px;">
                    <h2>Email Verification</h2>
                    <p>Thank you for registering. Please click the button below to verify your email address.</p>
                    <p>This link will expire in <strong>24 hours</strong>.</p>
                    <a href="%s"
                       style="display:inline-block; padding:12px 24px; background-color:#4CAF50;
                              color:white; text-decoration:none; border-radius:4px;">
                      Verify Email
                    </a>
                    <p>Or copy this link into your browser:</p>
                    <p><a href="%s">%s</a></p>
                    <p>If you did not create an account, please ignore this email.</p>
                  </body>
                </html>
                """.formatted(verificationLink, verificationLink, verificationLink);
    }

    private String buildPasswordResetBody(String resetLink) {
        return """
                <html>
                  <body style="font-family: Arial, sans-serif; padding: 20px;">
                    <h2>Password Reset</h2>
                    <p>We received a request to reset your password. Click the button below to proceed.</p>
                    <p>This link will expire in <strong>1 hour</strong>.</p>
                    <a href="%s"
                       style="display:inline-block; padding:12px 24px; background-color:#2196F3;
                              color:white; text-decoration:none; border-radius:4px;">
                      Reset Password
                    </a>
                    <p>Or copy this link into your browser:</p>
                    <p><a href="%s">%s</a></p>
                    <p>If you did not request a password reset, please ignore this email.</p>
                  </body>
                </html>
                """.formatted(resetLink, resetLink, resetLink);
    }
}
```

---

## auth/service/impl/AuthServiceImpl.java

```java
package com.example.test.auth.service.impl;

import com.example.test.auth.dto.request.*;
import com.example.test.auth.dto.response.AuthResponse;
import com.example.test.auth.dto.response.JwtAuthResponse;
import com.example.test.auth.entity.PasswordResetToken;
import com.example.test.auth.entity.RefreshToken;
import com.example.test.auth.entity.User;
import com.example.test.auth.entity.VerificationToken;
import com.example.test.auth.exception.EmailAlreadyExistsException;
import com.example.test.auth.exception.InvalidTokenException;
import com.example.test.auth.exception.TokenExpiredException;
import com.example.test.auth.exception.UserAlreadyVerifiedException;
import com.example.test.auth.repository.PasswordResetTokenRepository;
import com.example.test.auth.repository.RefreshTokenRepository;
import com.example.test.auth.repository.UserRepository;
import com.example.test.auth.repository.VerificationTokenRepository;
import com.example.test.auth.security.JwtService;
import com.example.test.auth.service.AuthService;
import com.example.test.auth.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final VerificationTokenRepository tokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Value("${app.jwt.refresh-expiration}")
    private long refreshExpiration;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already registered: " + request.getEmail());
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .enabled(false)
                .build();
        userRepository.save(user);

        String token = generateAndSaveVerificationToken(user);
        emailService.sendVerificationEmail(user.getEmail(), token);

        log.info("User registered: {}", user.getEmail());
        return AuthResponse.builder()
                .success(true)
                .message("Registration successful. Please check your email to verify your account.")
                .build();
    }

    @Override
    @Transactional
    public AuthResponse verifyEmail(String token) {
        VerificationToken verificationToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid verification token."));

        if (verificationToken.isExpired()) {
            tokenRepository.delete(verificationToken);
            throw new TokenExpiredException("Verification token has expired. Please request a new one.");
        }

        User user = verificationToken.getUser();
        if (user.isEnabled()) {
            throw new UserAlreadyVerifiedException("This account is already verified.");
        }

        user.setEnabled(true);
        userRepository.save(user);
        tokenRepository.delete(verificationToken);

        log.info("Email verified for user: {}", user.getEmail());
        return AuthResponse.builder()
                .success(true)
                .message("Email verified successfully. You can now log in.")
                .build();
    }

    @Override
    @Transactional
    public AuthResponse resendVerification(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidTokenException("No account found with email: " + email));

        if (user.isEnabled()) {
            throw new UserAlreadyVerifiedException("This account is already verified.");
        }

        tokenRepository.deleteByUser(user);

        String token = generateAndSaveVerificationToken(user);
        emailService.sendVerificationEmail(user.getEmail(), token);

        log.info("Verification email resent to: {}", user.getEmail());
        return AuthResponse.builder()
                .success(true)
                .message("Verification email resent. Please check your inbox.")
                .build();
    }

    @Override
    public JwtAuthResponse loginByEmail(LoginRequest request) {
        return authenticate(request.getEmail(), request.getPassword());
    }

    @Override
    public JwtAuthResponse loginByUsername(UsernameLoginRequest request) {
        return authenticate(request.getUsername(), request.getPassword());
    }

    @Override
    @Transactional
    public JwtAuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token."));

        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken);
            throw new TokenExpiredException("Refresh token has expired. Please log in again.");
        }

        User user = refreshToken.getUser();
        String newAccessToken = jwtService.generateToken(user);

        // rotate refresh token
        refreshTokenRepository.delete(refreshToken);
        String newRefreshToken = generateAndSaveRefreshToken(user);

        log.info("Token refreshed for user: {}", user.getEmail());
        return JwtAuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .role(user.getRole().name())
                .build();
    }

    @Override
    @Transactional
    public AuthResponse logout(RefreshTokenRequest request) {
        refreshTokenRepository.findByToken(request.getRefreshToken())
                .ifPresent(refreshTokenRepository::delete);

        return AuthResponse.builder()
                .success(true)
                .message("Logged out successfully.")
                .build();
    }

    @Override
    @Transactional
    public AuthResponse forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            passwordResetTokenRepository.deleteByUser(user);
            String token = UUID.randomUUID().toString();
            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .token(token)
                    .user(user)
                    .expiresAt(LocalDateTime.now().plusHours(1))
                    .build();
            passwordResetTokenRepository.save(resetToken);
            emailService.sendPasswordResetEmail(user.getEmail(), token);
            log.info("Password reset email sent to: {}", user.getEmail());
        });

        // always return success to prevent email enumeration
        return AuthResponse.builder()
                .success(true)
                .message("If that email is registered, a password reset link has been sent.")
                .build();
    }

    @Override
    @Transactional
    public AuthResponse resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new InvalidTokenException("Invalid password reset token."));

        if (resetToken.isExpired()) {
            passwordResetTokenRepository.delete(resetToken);
            throw new TokenExpiredException("Password reset token has expired. Please request a new one.");
        }

        if (resetToken.isUsed()) {
            throw new InvalidTokenException("Password reset token has already been used.");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        // invalidate all refresh tokens on password change
        refreshTokenRepository.deleteByUser(user);

        log.info("Password reset for user: {}", user.getEmail());
        return AuthResponse.builder()
                .success(true)
                .message("Password reset successfully. You can now log in with your new password.")
                .build();
    }

    private JwtAuthResponse authenticate(String identifier, String password) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(identifier, password)
        );

        User user = (User) authentication.getPrincipal();
        String accessToken = jwtService.generateToken(user);

        refreshTokenRepository.deleteByUser(user);
        String refreshToken = generateAndSaveRefreshToken(user);

        log.info("User logged in: {}", user.getEmail());
        return JwtAuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .role(user.getRole().name())
                .build();
    }

    private String generateAndSaveVerificationToken(User user) {
        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = VerificationToken.builder()
                .token(token)
                .user(user)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();
        tokenRepository.save(verificationToken);
        return token;
    }

    private String generateAndSaveRefreshToken(User user) {
        String token = UUID.randomUUID().toString();
        RefreshToken refreshToken = RefreshToken.builder()
                .token(token)
                .user(user)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshExpiration / 1000))
                .build();
        refreshTokenRepository.save(refreshToken);
        return token;
    }
}
```