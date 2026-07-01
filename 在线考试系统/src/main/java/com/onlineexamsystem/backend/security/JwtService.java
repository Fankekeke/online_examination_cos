package com.onlineexamsystem.backend.security;

import com.onlineexamsystem.backend.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final AppProperties appProperties;

    public JwtService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public String createAccessToken(Long userId) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(appProperties.getJwtAccessTokenExpiresMinutes(), ChronoUnit.MINUTES);
        return Jwts.builder()
            .subject(String.valueOf(userId))
            .issuedAt(Date.from(issuedAt))
            .expiration(Date.from(expiresAt))
            .signWith(getSigningKey())
            .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] bytes = appProperties.getJwtSecretKey().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(bytes.length >= 32 ? bytes : pad(bytes));
    }

    private byte[] pad(byte[] source) {
        byte[] padded = new byte[32];
        for (int i = 0; i < padded.length; i++) {
            padded[i] = source[i % source.length];
        }
        return padded;
    }
}
