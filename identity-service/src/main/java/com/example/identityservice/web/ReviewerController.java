package com.example.identityservice.web;

import com.example.identityservice.user.dto.CreatedAccount;
import com.example.identityservice.user.dto.UserProfileView;
import com.example.identityservice.user.service.ReviewerService;
import com.example.identityservice.web.dto.CreateUserRequest;
import com.example.identityservice.web.dto.DisableUserRequest;
import com.example.identityservice.web.dto.UpdateUserRequest;
import com.example.identityservice.web.dto.mapper.UserRequestMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * §2.3 Reviewer Management. {@code roleExpiresAt} on the create request is required unless the
 * caller holds {@code reviewer:grant_permanent} — grants default to time-boxed for this role.
 */
@RestController
@RequestMapping("/api/reviewers")
public class ReviewerController {

    private final ReviewerService reviewerService;
    private final RequestContextResolver requestContextResolver;

    public ReviewerController(ReviewerService reviewerService, RequestContextResolver requestContextResolver) {
        this.reviewerService = reviewerService;
        this.requestContextResolver = requestContextResolver;
    }

    @PostMapping
    public ResponseEntity<CreatedAccount> createReviewer(
            @Valid @RequestBody CreateUserRequest request,
            @RequestHeader("X-Actor-User-Id") Long actorUserId,
            HttpServletRequest httpRequest) {
        CreatedAccount account = reviewerService.createReviewer(
                UserRequestMapper.toCommand(request), actorUserId, requestContextResolver.resolve(httpRequest));
        return ResponseEntity.ok(account);
    }

    @PutMapping("/{userId}")
    public ResponseEntity<Void> updateReviewer(
            @PathVariable Long userId,
            @RequestBody UpdateUserRequest request,
            @RequestHeader("X-Actor-User-Id") Long actorUserId,
            HttpServletRequest httpRequest) {
        reviewerService.updateReviewer(
                userId, UserRequestMapper.toCommand(request), actorUserId, requestContextResolver.resolve(httpRequest));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{userId}/disable")
    public ResponseEntity<Void> disableReviewer(
            @PathVariable Long userId,
            @Valid @RequestBody DisableUserRequest request,
            @RequestHeader("X-Actor-User-Id") Long actorUserId) {
        reviewerService.disableReviewer(userId, request.mode(), request.reason(), actorUserId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileView> getProfile(
            @PathVariable Long userId,
            @RequestHeader("X-Actor-User-Id") Long actorUserId) {
        return ResponseEntity.ok(reviewerService.getProfile(userId, actorUserId));
    }
}
