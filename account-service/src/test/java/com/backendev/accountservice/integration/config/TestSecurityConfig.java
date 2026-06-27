package com.backendev.accountservice.integration.config;

import com.backendev.accountservice.jwt.JwtAuthenticationFilter;
import com.backendev.accountservice.jwt.JwtTokenValidator;
import com.backendev.accountservice.jwt.PublicEndpointMatcher;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.List;

@TestConfiguration
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class TestSecurityConfig {

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
            JwtTokenValidator tokenValidator,
            PublicEndpointMatcher publicEndpointMatcher) {

        return new JwtAuthenticationFilter(tokenValidator, publicEndpointMatcher) {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                            FilterChain filterChain) throws ServletException, IOException {
                // Only set authentication if not already set by @WithMockUser
                Authentication existingAuth = SecurityContextHolder.getContext().getAuthentication();
                if (existingAuth == null || existingAuth.getAuthorities().isEmpty()) {
                    List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken("user-123", null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }

                // Pass through without JWT validation
                filterChain.doFilter(request, response);
            }
        };
    }
}