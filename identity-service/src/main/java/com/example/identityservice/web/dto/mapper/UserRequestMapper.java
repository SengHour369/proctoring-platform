package com.example.identityservice.web.dto.mapper;

import com.example.identityservice.user.dto.CreateUserCommand;
import com.example.identityservice.user.dto.UpdateUserCommand;
import com.example.identityservice.web.dto.CreateUserRequest;
import com.example.identityservice.web.dto.UpdateUserRequest;

/** Translates the web-layer request bodies into the DTOs the {@code user.service} layer expects. */
public final class UserRequestMapper {

    private UserRequestMapper() {
    }

    public static CreateUserCommand toCommand(CreateUserRequest request) {
        return new CreateUserCommand(
                request.fullName(),
                request.email(),
                request.externalRef(),
                request.phoneNumber(),
                request.timeZone(),
                request.locale(),
                request.groupId(),
                request.roleExpiresAt());
    }

    public static UpdateUserCommand toCommand(UpdateUserRequest request) {
        UpdateUserCommand command = new UpdateUserCommand();
        command.setFullName(request.fullName());
        command.setPhoneNumber(request.phoneNumber());
        command.setTimeZone(request.timeZone());
        command.setLocale(request.locale());
        command.setEnrolmentPhotoPath(request.enrolmentPhotoPath());
        command.setVoiceprintPath(request.voiceprintPath());
        command.setEmail(request.email());
        command.setExternalRef(request.externalRef());
        command.setReason(request.reason());
        return command;
    }
}
