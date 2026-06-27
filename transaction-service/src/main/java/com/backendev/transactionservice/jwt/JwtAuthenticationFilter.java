package com.backendev.transactionservice.jwt;

import com.backendev.transactionservice.exception.JwtAuthenticationException;
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

    private final JwtTokenValidator tokenValidator;
    private final PublicEndpointMatcher publicEndpointMatcher;

    public JwtAuthenticationFilter(JwtTokenValidator tokenValidator, PublicEndpointMatcher publicEndpointMatcher) {
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

        try {
            String token = extractTokenFromRequest(request);
            if (token == null) {
                log.warn("No JWT token found for protected endpoint: {}", requestPath);
                throw new JwtAuthenticationException("Missing or invalid Authorization header");
            }

            Authentication authentication = tokenValidator.validateTokenAndCreateAuthentication(token);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("Successfully authenticated user: {} for path: {}",
                    authentication.getName(), requestPath);

        } catch (JwtAuthenticationException e) {
            log.warn("JWT authentication failed for {}: {}", requestPath, e.getMessage());
            writeErrorResponse(response, e.getMessage());
            return;
        } catch (Exception e) {
            log.error("Unexpected error during authentication for {}", requestPath, e);
            writeErrorResponse(response, "Authentication error");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private void writeErrorResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(String.format("{\"error\":\"Unauthorized\",\"message\":\"%s\"}", message));
    }
}
