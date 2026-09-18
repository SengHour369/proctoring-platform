package com.example.identityservice.web;

import com.example.identityservice.user.dto.UserActivityPage;
import com.example.identityservice.user.service.UserActivityService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/** §2.5 User Activity — a read model, not a table; see {@link UserActivityService}. */
@RestController
@RequestMapping("/api/users/{userId}/activity")
public class UserActivityController {

    private final UserActivityService userActivityService;

    public UserActivityController(UserActivityService userActivityService) {
        this.userActivityService = userActivityService;
    }

    @GetMapping
    public ResponseEntity<UserActivityPage> getActivity(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestHeader("X-Actor-User-Id") Long actorUserId) {
        return ResponseEntity.ok(userActivityService.getActivity(userId, from, to, page, size, actorUserId));
    }
}
