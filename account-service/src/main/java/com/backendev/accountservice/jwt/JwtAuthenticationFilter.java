package com.backendev.accountservice.jwt;

import com.backendev.accountservice.exception.JwtAuthenticationException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String UNAUTHORIZED_ERROR_FORMAT = "{\"error\":\"Unauthorized\",\"message\":\"%s\"}";

    private final JwtTokenValidator tokenValidator;
    private final PublicEndpointMatcher publicEndpointMatcher;

    protected JwtAuthenticationFilter(JwtTokenValidator tokenValidator, PublicEndpointMatcher publicEndpointMatcher) {
        this.tokenValidator = tokenValidator;
        this.publicEndpointMatcher = publicEndpointMatcher;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestPath = request.getRequestURI();
        String method = request.getMethod();

        log.info("JWT Filter Processing request: {} {}", method, requestPath);

        if (publicEndpointMatcher.isPublicEndpoint(requestPath)) {
            log.debug("Public endpoint accessed: {}", requestPath);
            filterChain.doFilter(request, response);
            return;
        }
        log.info("Processing PROTECTED endpoint: {}", requestPath);

        if (!authenticateRequest(request, requestPath)) {
            writeErrorResponse(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(7);
        }
        return null;
    }

    private boolean authenticateRequest(HttpServletRequest request, String requestPath) {
        try {
            final String token = extractTokenFromRequest(request);

            if (token == null) {
                log.warn("No JWT token found for protected endpoint: {}", requestPath);
                return false;
            }

            final Authentication authentication = tokenValidator.validateTokenAndCreateAuthentication(token);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("Successfully authenticated user: {} for path: {}",
                    authentication.getName(), requestPath);
            return true;

        } catch (JwtAuthenticationException e) {
            log.warn("JWT authentication failed for {}: {}", requestPath, e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Unexpected error during authentication for {}", requestPath, e);
            return false;
        }
    }

    private void writeErrorResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(CONTENT_TYPE_JSON);
        response.getWriter().write(String.format(UNAUTHORIZED_ERROR_FORMAT, "Authentication failed"));
    }
}