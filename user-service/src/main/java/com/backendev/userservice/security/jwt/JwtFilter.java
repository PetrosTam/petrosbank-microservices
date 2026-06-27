package com.backendev.userservice.security.jwt;

import com.backendev.userservice.security.AppUserServiceDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;


@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AppUserServiceDetails userServiceDetails;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final int BEARER_PREFIX_LENGTH = BEARER_PREFIX.length();

    //Bypass JWT filter for Swagger-related endpoints
    private static final Set<String> BYPASS_PATHS = Set.of(
            "/v3/api-docs",
            "/swagger-ui",
            "/swagger-resources",
            "/configuration"
    );

    private static final Logger log = LoggerFactory.getLogger(JwtFilter.class);


    public JwtFilter(JwtService jwtService, AppUserServiceDetails userServiceDetails) {
        this.jwtService = jwtService;
        this.userServiceDetails = userServiceDetails;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (shouldBypassJwtProcessing(request)) {
            log.debug("Bypassing JWT processing for path: {}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }
        String jwtToken = extractTokenFromRequest(request);

        if (jwtToken != null && !isAlreadyAuthenticated()) {
            authenticateUser(jwtToken, request);
        }
        filterChain.doFilter(request, response);
    }

    private boolean shouldBypassJwtProcessing(HttpServletRequest request) {
        String requestPath = request.getRequestURI();
        return BYPASS_PATHS.stream().anyMatch(requestPath::startsWith);
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        log.debug("Authorization header: {}", authHeader);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.debug("No valid Authorization header found");
            return null;
        }
        if (authHeader.length() <= BEARER_PREFIX_LENGTH) {
            log.debug("Authorization header contains only Bearer prefix");
            return null;
        }

        String token = authHeader.substring(BEARER_PREFIX_LENGTH).trim();
        if (token.isEmpty()) {
            log.debug("Empty token found in Authorization header");
            return null;
        }
        log.debug("JWT token extracted successfully");
        return token;
    }

    private boolean isAlreadyAuthenticated() {
        return SecurityContextHolder.getContext().getAuthentication() != null;
    }


    private void authenticateUser(String token, HttpServletRequest request) {
        try {
            String username = extractUsernameFromToken(token);
            if (username == null) {
                log.debug("Could not extract username from token");
                return;
            }
            log.debug("Username extracted from token: {}", username);

            UserDetails userDetails = loadUserDetails(username);
            if (userDetails == null) {
                log.warn("User details not found for username: {}", username);
                return;
            }
            if (!isTokenValid(token)) {
                log.warn("Token validation failed for user: {}", username);
                return;
            }
            setAuthentication(userDetails, request);
            log.info("User successfully authenticated: {}", username);

        } catch (Exception e) {
            log.error("Authentication failed: {}", e.getMessage(), e);
        }
    }

    private String extractUsernameFromToken(String token) {
        try {
            return jwtService.extractUsername(token);
        } catch (Exception e) {
            log.error("Failed to extract username from JWT token: {}", e.getMessage());
            return null;
        }
    }

    private UserDetails loadUserDetails(String username) {
        try {
            return userServiceDetails.loadUserByUsername(username);
        } catch (Exception e) {
            log.error("Failed to load user details for username {}: {}", username, e.getMessage());
            return null;
        }
    }

    private boolean isTokenValid(String token) {
        try {
            return jwtService.validateToken(token);
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    private void setAuthentication(UserDetails userDetails, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );

        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
        log.debug("Authentication set in SecurityContext for user: {}", userDetails.getUsername());
    }
}
