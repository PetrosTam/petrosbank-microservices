package com.backendev.userservice.security.jwt;

import com.backendev.userservice.audit.AuditEventType;
import com.backendev.userservice.exception.TokenExpiredException;
import com.backendev.userservice.service.AuditService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class JwtService {

    private final AuditService auditService;

    @Value("${JWT_SECRET}")
    private String secretKey;

    @Value("${security.jwt.access-token-expiration-ms:900000}")
    private long accessTokenExpirationMs;

    public JwtService(AuditService auditService) {
        this.auditService = auditService;
    }

    public String generateToken(
            String username,
            Map<String, Object> claims
    ) {
        Date issuedAt = new Date();
        Date expiration = new Date(
                issuedAt.getTime() + accessTokenExpirationMs
        );

        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .id(UUID.randomUUID().toString())
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(getSigningKey())
                .compact();
    }

    public String extractUsername(String token) {
        return getClaims(token).getSubject();
    }

    public String extractTokenId(String token) {
        return getClaims(token).getId();
    }

    public Date extractExpiration(String token) {
        return getClaims(token).getExpiration();
    }

    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    private Claims getClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException exception) {
            String email = exception.getClaims() != null
                    ? exception.getClaims().getSubject()
                    : "unknown";

            auditService.auditLog(
                    AuditEventType.TOKEN_EXPIRED,
                    email,
                    "Token expired"
            );

            log.warn("JWT expired for subject={}", email);

            throw new TokenExpiredException(
                    "JWT expired. Please log in again.",
                    exception
            );
        }
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Base64.getDecoder().decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}