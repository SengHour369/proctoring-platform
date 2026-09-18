package com.example.examservice.exam.dto;

import com.example.examservice.exam.enums.InvitationChannel;
import jakarta.validation.constraints.NotNull;

public record SendInvitationRequest(@NotNull InvitationChannel channel) {
}
