package com.backendev.accountservice.unit.jwt;

import com.backendev.accountservice.exception.JwtAuthenticationException;
import com.backendev.accountservice.jwt.JwtTokenValidator;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenValidatorTest {

    private JwtTokenValidator validator;
    private SecretKey signingKey;

    private static final String DEFAULT_USER_ID = "123";


    @BeforeEach
    void setUp() {
        signingKey = Jwts.SIG.HS256.key().build();
        String secretKeyString = Base64.getEncoder().encodeToString(signingKey.getEncoded());
        validator = new JwtTokenValidator(secretKeyString);
    }

    @Test
    void shouldValidateTokenWithUserIdAsString() {
        String token = createToken(List.of("USER", "ADMIN"));

        Authentication auth = validator.validateTokenAndCreateAuthentication(token);

        assertThat(auth.getName()).isEqualTo("123");
        assertThat(auth.getAuthorities()).hasSize(2);
        assertThat(auth.getAuthorities().stream().map(GrantedAuthority::getAuthority))
                .containsExactly("USER", "ADMIN");
    }

    @Test
    void shouldValidateTokenWithUserIdAsInteger() {
        String token = createTokenWithIntegerId(List.of("USER"));

        Authentication auth = validator.validateTokenAndCreateAuthentication(token);

        assertThat(auth.getName()).isEqualTo("123");
        assertThat(auth.getAuthorities()).hasSize(1);
    }

    @Test
    void shouldFallbackToSubjectWhenUserIdMissing() {
        String token = Jwts.builder()
                .subject("user@example.com")
                .claim("roles", List.of("USER"))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(signingKey)
                .compact();

        Authentication auth = validator.validateTokenAndCreateAuthentication(token);

        assertThat(auth.getName()).isEqualTo("user@example.com");
    }

    @Test
    void shouldHandleEmptyRoles() {
        String token = createToken(List.of());

        Authentication auth = validator.validateTokenAndCreateAuthentication(token);

        assertThat(auth.getAuthorities()).isEmpty();
    }

    @Test
    void shouldRejectExpiredToken() {
        String token = Jwts.builder()
                .claim("userId", "123")
                .issuedAt(new Date(System.currentTimeMillis() - 7200000)) // 2 hours ago
                .expiration(new Date(System.currentTimeMillis() - 3600000)) // 1 hour ago
                .signWith(signingKey)
                .compact();

        assertThatThrownBy(() -> validator.validateTokenAndCreateAuthentication(token))
                .isInstanceOf(JwtAuthenticationException.class)
                .hasMessageContaining("Token expired");
    }

    @Test
    void shouldRejectMalformedToken() {
        assertThatThrownBy(() -> validator.validateTokenAndCreateAuthentication("invalid.token"))
                .isInstanceOf(JwtAuthenticationException.class)
                .hasMessageContaining("Malformed token");
    }

    @Test
    void shouldRejectTokenWithoutUserId() {
        String token = Jwts.builder()
                .claim("roles", List.of("USER"))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(signingKey)
                .compact();

        assertThatThrownBy(() -> validator.validateTokenAndCreateAuthentication(token))
                .isInstanceOf(JwtAuthenticationException.class)
                .hasMessageContaining("missing user information");
    }

    @Test
    void shouldThrowExceptionForInvalidSecretKey() {
        assertThatThrownBy(() -> new JwtTokenValidator("invalid-key"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid JWT secret key configuration");
    }

    private String createToken(List<String> roles) {
        return Jwts.builder()
                .claim("userId", DEFAULT_USER_ID)
                .claim("roles", roles)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(signingKey)
                .compact();
    }

    private String createTokenWithIntegerId(List<String> roles) {
        return Jwts.builder()
                .claim("userId", DEFAULT_USER_ID)
                .claim("roles", roles)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(signingKey)
                .compact();
    }
}