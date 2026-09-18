package com.example.identityservice.user.dto;

import java.util.List;

public record UserActivityPage(List<ActivityEntry> entries, int page, int size, boolean hasMore) {
}
