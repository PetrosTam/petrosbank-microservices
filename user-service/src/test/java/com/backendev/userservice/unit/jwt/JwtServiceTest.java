package com.backendev.userservice.unit.jwt;

import com.backendev.userservice.audit.AuditEventType;
import com.backendev.userservice.exception.TokenExpiredException;
import com.backendev.userservice.security.jwt.JwtService;
import com.backendev.userservice.service.AuditService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @Mock
    private AuditService auditService;

    @InjectMocks
    private JwtService jwtService;

    private String testSecretKey;
    private String testUsername;
    private Map<String, Object> testClaims;
    private static final long TOKEN_EXPIRATION_TIME = 900000; // 15 minutes

    @BeforeEach
    void setUp(){
        testSecretKey = Base64.getEncoder().encodeToString(
                "mySecretKeyForTestingJwtTokensWithSufficientLength123".getBytes()
        );

        ReflectionTestUtils.setField(jwtService, "secretKey", testSecretKey);

        testUsername = "test@example.com";
        testClaims = new HashMap<>();
        testClaims.put("role", "USER");
        testClaims.put("userId", 123L);
    }

    @Test
    void generateToken_ShouldReturnValidJwtToken_WhenValidInputProvided() {
        
        String token = jwtService.generateToken(testUsername, testClaims);

        assertNotNull(token);

        String[] parts = token.split("\\.");
        assertEquals(3, parts.length, "JWT should have exactly 3 parts (header.payload.signature)");
        assertFalse(parts[0].isEmpty(), "JWT header should not be empty");
        assertFalse(parts[1].isEmpty(), "JWT payload should not be empty");
        assertFalse(parts[2].isEmpty(), "JWT signature should not be empty");

        assertDoesNotThrow(() -> {
            String extractedUsername = jwtService.extractUsername(token);
            assertEquals(testUsername, extractedUsername);
        }, "Generated token should be parseable and return correct username");
    }

    @Test
    void generateToken_ShouldIncludeAllClaims_WhenClaimsProvided() {
        
        String token = jwtService.generateToken(testUsername, testClaims);

        SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(testSecretKey));
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertEquals(testUsername, claims.getSubject());
        assertEquals("USER", claims.get("role"));
        assertEquals(123L, ((Number) claims.get("userId")).longValue());
    }

    @Test
    void generateToken_ShouldSetCorrectExpiration_WhenTokenGenerated() {
        
        String token = jwtService.generateToken(testUsername, testClaims);

        Date expiration = jwtService.extractExpiration(token);
        long currentTime = System.currentTimeMillis();
        
        long timingBuffer = 5000;
        long minExpectedExpiration = currentTime + TOKEN_EXPIRATION_TIME - timingBuffer;
        long maxExpectedExpiration = currentTime + TOKEN_EXPIRATION_TIME + timingBuffer;

        assertTrue(expiration.getTime() >= minExpectedExpiration,
                String.format("Token expiration (%d) should be at least %d ms from current time",
                        expiration.getTime(), minExpectedExpiration));
        assertTrue(expiration.getTime() <= maxExpectedExpiration,
                String.format("Token expiration (%d) should be at most %d ms from current time",
                        expiration.getTime(), maxExpectedExpiration));

        assertTrue(expiration.after(new Date()), "Token should expire in the future");
    }

    @Test
    void generateToken_ShouldHandleEmptyClaims_WhenNoAdditionalClaimsProvided() {
        Map<String, Object> emptyClaims = new HashMap<>();
        
        String token = jwtService.generateToken(testUsername, emptyClaims);

        assertNotNull(token);
        assertEquals(testUsername, jwtService.extractUsername(token));
    }
    
    @Test
    void extractUsername_ShouldReturnCorrectUsername_WhenValidTokenProvided() {
        String token = jwtService.generateToken(testUsername, testClaims);
        String extractedUsername = jwtService.extractUsername(token);
        assertEquals(testUsername, extractedUsername);
    }

    @Test
    void extractUsername_ShouldThrowTokenExpiredException_WhenExpiredTokenProvided() {
        String expiredToken = createExpiredToken();

        assertThrows(TokenExpiredException.class, () ->
            jwtService.extractUsername(expiredToken));
    }

    @Test
    void validateToken_ShouldReturnTrue_WhenValidTokenProvided() {
        String validToken = jwtService.generateToken(testUsername, testClaims);
        boolean isValid = jwtService.validateToken(validToken);
        assertTrue(isValid);
    }

    @Test
    void validateToken_ShouldReturnFalse_WhenExpiredTokenProvided() {
        String expiredToken = createExpiredToken();
        boolean isValid = jwtService.validateToken(expiredToken);
        assertFalse(isValid);
    }

    @Test
    void validateToken_ShouldReturnFalse_WhenInvalidTokenProvided() {
        String invalidToken = "invalid.jwt.token";
        boolean isValid = jwtService.validateToken(invalidToken);
        assertFalse(isValid);
    }

    @Test
    void validateToken_ShouldReturnFalse_WhenNullTokenProvided() {
        boolean isValid = jwtService.validateToken(null);
        assertFalse(isValid);
    }

    @Test
    void validateToken_ShouldReturnFalse_WhenEmptyTokenProvided() {
        boolean isValid = jwtService.validateToken("");
        assertFalse(isValid);
    }

    @Test
    void extractExpiration_ShouldReturnCorrectDate_WhenValidTokenProvided() {
        long beforeGeneration = System.currentTimeMillis();
        String token = jwtService.generateToken(testUsername, testClaims);
        long afterGeneration = System.currentTimeMillis();

        Date expiration = jwtService.extractExpiration(token);

        assertNotNull(expiration);
        assertTrue(expiration.after(new Date(beforeGeneration + TOKEN_EXPIRATION_TIME - 1000)));
        assertTrue(expiration.before(new Date(afterGeneration + TOKEN_EXPIRATION_TIME + 1000)));
    }

    @Test
    void extractExpiration_ShouldThrowTokenExpiredException_WhenExpiredTokenProvided() {
        String expiredToken = createExpiredToken();
        assertThrows(TokenExpiredException.class, () ->
            jwtService.extractExpiration(expiredToken));
    }

    @Test
    void getClaims_ShouldAuditTokenExpiration_WhenTokenExpired() {
        String expiredToken = createExpiredToken();
        assertThrows(TokenExpiredException.class, () -> jwtService.extractUsername(expiredToken));

        verify(auditService).auditLog(AuditEventType.TOKEN_EXPIRED,testUsername,"Token expired");
    }

    @Test
    void getClaims_ShouldHandleMalformedToken_WhenInvalidTokenStructure() {
        String malformedToken = "not.a.valid.jwt.token.structure";
        boolean isValid = jwtService.validateToken(malformedToken);
        assertFalse(isValid);
    }

    @Test
    void getClaims_ShouldHandleTokenWithoutSubject_WhenExpiredTokenHasNoClaims() {
        String expiredToken = createExpiredToken();
        assertThrows(TokenExpiredException.class, () ->
            jwtService.extractUsername(expiredToken));
    }

    @Test
    void validateToken_ShouldHandleJwtParsingException_WhenTokenIsCorrupted() {
        String[] malformedTokens = {
                "corrupted-token",
                "header.corrupted.signature",
                "",
                "   ",
                "header.payload", // Missing signature
                "header.payload.signature.extra", // Too many parts
                "header..signature" // Empty payload
        };

        for (String token : malformedTokens) {
            boolean isValid = jwtService.validateToken(token);
            assertFalse(isValid, "Token should be invalid: " + token);
        }
    }

    @Test
    void getSignedKey_ShouldHandleKeyGeneration_WhenCalled() {
        String token1 = jwtService.generateToken(testUsername, testClaims);
        String token2 = jwtService.generateToken("another@test.com", testClaims);

        assertNotNull(token1);
        assertNotNull(token2);
        assertTrue(jwtService.validateToken(token1));
        assertTrue(jwtService.validateToken(token2));
    }

    @Test
    void extractUsername_ShouldHandleValidTokenMultipleTimes() {
        String token = jwtService.generateToken(testUsername, testClaims);

        String username1 = jwtService.extractUsername(token);
        String username2 = jwtService.extractUsername(token);
        String username3 = jwtService.extractUsername(token);

        assertEquals(testUsername, username1);
        assertEquals(testUsername, username2);
        assertEquals(testUsername, username3);
    }

    @Test
    void extractExpiration_ShouldHandleValidTokenMultipleTimes() {
        String token = jwtService.generateToken(testUsername, testClaims);

        Date exp1 = jwtService.extractExpiration(token);
        Date exp2 = jwtService.extractExpiration(token);

        assertEquals(exp1, exp2);
        assertTrue(exp1.after(new Date()));
    }


    @Test
    void tokenLifecycle_ShouldWorkEndToEnd_WhenValidOperations() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "ADMIN");
        claims.put("permissions", "READ,WRITE");

        String token = jwtService.generateToken(testUsername, claims);

        String extractedUsername = jwtService.extractUsername(token);
        Date expiration = jwtService.extractExpiration(token);
        boolean isValid = jwtService.validateToken(token);

        assertEquals(testUsername, extractedUsername);
        assertNotNull(expiration);
        assertTrue(expiration.after(new Date()));
        assertTrue(isValid);
    }

    private String createExpiredToken() {
        SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(testSecretKey));
        Date pastDate = new Date(System.currentTimeMillis() - 1000); // 1 second ago

        return Jwts.builder()
                .subject(testUsername)
                .claims(testClaims)
                .issuedAt(new Date(System.currentTimeMillis() - 2000))
                .expiration(pastDate)
                .signWith(key)
                .compact();
    }

    @Test
    void generateToken_ShouldHandleLongUsername_WhenLongUsernameProvided() {
        String longUsername = "very.long.username.that.might.cause.issues@verylongdomainname.com";
        String token = jwtService.generateToken(longUsername, testClaims);
        assertNotNull(token);
        assertEquals(longUsername, jwtService.extractUsername(token));
    }

    @Test
    void generateToken_ShouldHandleSpecialCharacters_WhenSpecialCharsInUsername() {
        String specialUsername = "user+test@domain-name.com";
        String token = jwtService.generateToken(specialUsername, testClaims);
        assertNotNull(token);
        assertEquals(specialUsername, jwtService.extractUsername(token));
    }

    @Test
    void generateToken_ShouldHandleLargeClaims_WhenManyClaimsProvided() {
        Map<String, Object> largeClaims = new HashMap<>();
        for (int i = 0; i < 50; i++) {
            largeClaims.put("claim" + i, "value" + i);
        }
        String token = jwtService.generateToken(testUsername, largeClaims);
        assertNotNull(token);
        assertTrue(jwtService.validateToken(token));
    }
}