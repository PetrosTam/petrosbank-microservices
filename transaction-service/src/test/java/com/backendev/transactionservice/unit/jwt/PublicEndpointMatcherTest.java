package com.backendev.transactionservice.unit.jwt;

import com.backendev.transactionservice.jwt.PublicEndpointMatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class PublicEndpointMatcherTest {

    private PublicEndpointMatcher matcher;

    @BeforeEach
    void setUp() {
        matcher = new PublicEndpointMatcher();
    }

    @Test
    void shouldReturnTrueForExactPublicPath() {
        assertTrue(matcher.isPublicEndpoint("/api/v1/transaction/debug-auth"));
    }

    @Test
    void shouldReturnTrueForPublicPrefixPaths() {
        assertTrue(matcher.isPublicEndpoint("/api/public"));
        assertTrue(matcher.isPublicEndpoint("/api/public/users"));
        assertTrue(matcher.isPublicEndpoint("/actuator"));
        assertTrue(matcher.isPublicEndpoint("/actuator/health"));
        assertTrue(matcher.isPublicEndpoint("/v3/api-docs"));
        assertTrue(matcher.isPublicEndpoint("/v3/api-docs/swagger-config"));
        assertTrue(matcher.isPublicEndpoint("/swagger-ui"));
        assertTrue(matcher.isPublicEndpoint("/swagger-ui/index.html"));
    }

    @Test
    void shouldReturnFalseForPrivatePaths() {
        assertFalse(matcher.isPublicEndpoint("/api/v1/users"));
        assertFalse(matcher.isPublicEndpoint("/api/secure"));
        assertFalse(matcher.isPublicEndpoint("/api/v1/transaction/create"));
    }

    @Test
    void shouldReturnFalseForNearMatchPaths() {
        assertFalse(matcher.isPublicEndpoint("/api/v1/transaction/debug-auth-test"));
        assertFalse(matcher.isPublicEndpoint("/public"));
        assertFalse(matcher.isPublicEndpoint("/api"));
    }

    @Test
    void shouldReturnFalseForEmptyPath() {
        assertFalse(matcher.isPublicEndpoint(""));
    }

}