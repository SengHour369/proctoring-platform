package com.example.identityservice.web;

import com.example.identityservice.user.dto.CreatedAccount;
import com.example.identityservice.user.dto.UserProfileView;
import com.example.identityservice.user.service.StudentService;
import com.example.identityservice.web.dto.CreateUserRequest;
import com.example.identityservice.web.dto.DisableUserRequest;
import com.example.identityservice.web.dto.UpdateUserRequest;
import com.example.identityservice.web.dto.mapper.UserRequestMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * §2.1 Student Management. {@code X-Actor-User-Id} stands in for the authenticated caller — see
 * {@link SecurityConfig} for why there is no JWT resource filter reading it yet.
 */
@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService studentService;
    private final RequestContextResolver requestContextResolver;

    public StudentController(StudentService studentService, RequestContextResolver requestContextResolver) {
        this.studentService = studentService;
        this.requestContextResolver = requestContextResolver;
    }

    @PostMapping
    public ResponseEntity<CreatedAccount> createStudent(
            @Valid @RequestBody CreateUserRequest request,
            @RequestHeader("X-Actor-User-Id") Long actorUserId,
            HttpServletRequest httpRequest) {
        CreatedAccount account = studentService.createStudent(
                UserRequestMapper.toCommand(request), actorUserId, requestContextResolver.resolve(httpRequest));
        return ResponseEntity.ok(account);
    }

    @PutMapping("/{userId}")
    public ResponseEntity<Void> updateStudent(
            @PathVariable Long userId,
            @RequestBody UpdateUserRequest request,
            @RequestHeader("X-Actor-User-Id") Long actorUserId,
            HttpServletRequest httpRequest) {
        studentService.updateStudent(
                userId, UserRequestMapper.toCommand(request), actorUserId, requestContextResolver.resolve(httpRequest));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{userId}/disable")
    public ResponseEntity<Void> disableStudent(
            @PathVariable Long userId,
            @Valid @RequestBody DisableUserRequest request,
            @RequestHeader("X-Actor-User-Id") Long actorUserId) {
        studentService.disableStudent(userId, request.mode(), request.reason(), actorUserId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileView> getProfile(
            @PathVariable Long userId,
            @RequestHeader("X-Actor-User-Id") Long actorUserId) {
        return ResponseEntity.ok(studentService.getProfile(userId, actorUserId));
    }
}
