package com.example.identityservice.web.dto;

import java.util.List;

public record AuthenticateClientResponse(String clientId, List<String> allowedScopes) {
}
