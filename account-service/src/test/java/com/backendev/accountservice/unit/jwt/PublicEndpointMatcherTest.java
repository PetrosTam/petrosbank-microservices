package com.backendev.accountservice.unit.jwt;

import com.backendev.accountservice.jwt.PublicEndpointMatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class PublicEndpointMatcherTest {

    private PublicEndpointMatcher publicEndpointMatcher;

    @BeforeEach
    void setUp() {
        publicEndpointMatcher = new PublicEndpointMatcher();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/v1/accounts/debug-auth",
            "/api/v1/accounts/ping",
            "/api/public",
            "/api/public/users",
            "/actuator/health",
            "/v3/api-docs/config",
            "/swagger-ui/index.html"
    })
    void shouldMatchPublicEndpoints(String path) {
        assertThat(publicEndpointMatcher.isPublicEndpoint(path)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/v1/users",
            "/api/v1/accounts/create",
            "/private/endpoint",
            "/some/api/public",
            "/API/PUBLIC/test",
            ""
    })
    void shouldNotMatchPrivateEndpoints(String path) {
        assertThat(publicEndpointMatcher.isPublicEndpoint(path)).isFalse();
    }

        @Test
        void shouldHandleNullPath() {
            assertThat(publicEndpointMatcher.isPublicEndpoint(null)).isFalse();
            assertThat(publicEndpointMatcher.isPublicEndpoint("")).isFalse();
    }

}