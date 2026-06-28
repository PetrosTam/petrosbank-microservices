package com.backendev.userservice.service;

import com.backendev.userservice.audit.AuditEventType;
import com.backendev.userservice.dto.AuthRequest;
import com.backendev.userservice.dto.AuthResponse;
import com.backendev.userservice.entity.RefreshToken;
import com.backendev.userservice.entity.Users;
import com.backendev.userservice.security.AppUserDetails;
import com.backendev.userservice.security.jwt.JwtService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@AllArgsConstructor
public class LoginService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AuditService auditService;
    private final RefreshTokenService refreshTokenService;

    public AuthResponse login(AuthRequest authRequest) {
        AppUserDetails userDetails = authenticate(authRequest);
        String accessToken = createJwtToken(userDetails);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(userDetails.getUsername());

        AuthResponse authResponse = buildAuthResponseFromUserDetails(
                accessToken,
                refreshToken.getToken(),
                userDetails
        );

        auditService.auditLog(AuditEventType.LOGIN_SUCCESS, authRequest.getEmail(), "Login successful");
        return authResponse;
    }

    @Transactional
    public AuthResponse refreshAccessToken(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenService.validateRefreshToken(refreshTokenValue);

        Users user = refreshToken.getUser();
        AppUserDetails userDetails = new AppUserDetails(user);

        String newAccessToken = createJwtToken(userDetails);

        return buildAuthResponseFromUserDetails(
                newAccessToken,
                refreshToken.getToken(),
                userDetails
        );
    }

    public void logout(String refreshTokenValue) {
        refreshTokenService.revokeRefreshToken(refreshTokenValue);
    }

    private AppUserDetails authenticate(AuthRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        if (!authentication.isAuthenticated()) {
            log.error("Login Service: Failed to authenticate");
            throw new BadCredentialsException("Invalid credentials");
        }

        return (AppUserDetails) authentication.getPrincipal();
    }

    private String createJwtToken(AppUserDetails userDetails) {
        Map<String, Object> claims = buildClaims(userDetails);
        return jwtService.generateToken(userDetails.getUsername(), claims);
    }

    private Map<String, Object> buildClaims(AppUserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userDetails.getId());
        claims.put("roles", userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList());
        return claims;
    }

    private AuthResponse buildAuthResponseFromUserDetails(
            String accessToken,
            String refreshToken,
            AppUserDetails userDetails
    ) {
        return new AuthResponse(
                accessToken,
                refreshToken,
                userDetails.getUsername(),
                userDetails.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList(),
                jwtService.extractExpiration(accessToken)
        );
    }
}