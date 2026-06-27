package com.backendev.transactionservice.jwt;

import com.backendev.transactionservice.exception.JwtAuthenticationException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class JwtTokenValidator {
    private final String secretKey;
    private final Key signingKey;

    public JwtTokenValidator(@Value("${JWT_SECRET}") String secretKey) {
        this.secretKey = secretKey;
        this.signingKey = createSigningKey();
        log.info("JWT Token Validator initialized with key length: {}",
                secretKey != null ? secretKey.length() : 0);
    }

    public Authentication validateTokenAndCreateAuthentication(String token) throws JwtAuthenticationException {
        try {
            // Parse and validate JWT token
            Claims claims = Jwts.parser()
                    .verifyWith((SecretKey) signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            log.debug("JWT token validated successfully");

            // Extract user information
            String userId = extractUserId(claims);
            List<GrantedAuthority> authorities = extractAuthorities(claims);

            if (userId == null) {
                throw new JwtAuthenticationException("Invalid token: missing user information");
            }

            // Create Spring Security authentication object
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);

            // Store JWT claims for potential later use
            authentication.setDetails(claims);

            return authentication;

        } catch (ExpiredJwtException e) {
            throw new JwtAuthenticationException("Token expired", e);
        } catch (MalformedJwtException e) {
            throw new JwtAuthenticationException("Malformed token", e);
        } catch (JwtException e) {
            throw new JwtAuthenticationException("Invalid token: " + e.getMessage(), e);
        }
    }

    /**
     * Creates signing key from base64-encoded secret
     */
    private Key createSigningKey() {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(this.secretKey);
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (Exception e) {
            log.error("Failed to create JWT signing key", e);
            throw new IllegalStateException("Invalid JWT secret key configuration");
        }
    }

    /**
     * Extracts user ID from JWT claims
     */
    private String extractUserId(Claims claims) {
        Object userIdObj = claims.get("userId");
        if (userIdObj instanceof Integer) {
            return String.valueOf(userIdObj);
        } else if (userIdObj instanceof String userIdString) {
            return userIdString;
        }

        // Fallback to subject claim
        String subject = claims.getSubject();
        log.info("UserId fallback to subject: {}", subject);
        return subject;
    }

    /**
     * Extracts user authorities/roles from JWT claims
     */
    private List<GrantedAuthority> extractAuthorities(Claims claims) {
        Object rolesObj = claims.get("roles");
        if (rolesObj instanceof List<?> rolesList) {
            return rolesList.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
