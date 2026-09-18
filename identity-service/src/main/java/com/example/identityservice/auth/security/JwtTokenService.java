package com.example.identityservice.auth.security;

import com.example.identityservice.auth.config.AuthProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

/** Issues short-lived access tokens scoped to a user id. Refresh tokens are opaque, not JWTs. */
@Component
public class JwtTokenService {

    private final SecretKey signingKey;
    private final AuthProperties properties;

    public JwtTokenService(@Value("${app.auth.jwt-secret}") String jwtSecret, AuthProperties properties) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
        this.properties = properties;
    }

    public String generateAccessToken(Long userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.getAccessTokenTtl())))
                .signWith(signingKey)
                .compact();
    }
}
