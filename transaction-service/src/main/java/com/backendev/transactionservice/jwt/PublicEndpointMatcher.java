package com.backendev.transactionservice.jwt;

import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class PublicEndpointMatcher {
    private final Set<String> publicPaths = Set.of(
            "/api/v1/transaction/debug-auth"
    );

    private final Set<String> publicPrefixes = Set.of(
            "/api/public",
            "/actuator",
            "/v3/api-docs",
            "/swagger-ui"
    );

    public boolean isPublicEndpoint(String path) {
        return publicPaths.contains(path) ||
                publicPrefixes.stream().anyMatch(path::startsWith);
    }
}
