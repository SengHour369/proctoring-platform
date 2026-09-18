package com.example.identityservice.web;

import com.example.identityservice.auth.dto.RequestContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class RequestContextResolver {

    public RequestContext resolve(HttpServletRequest request) {
        return new RequestContext(
                clientIp(request),
                request.getHeader("User-Agent"),
                request.getHeader("X-Device-Fingerprint"),
                request.getHeader("X-Geo-Country"));
    }

    public String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
