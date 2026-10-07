package org.os4all.core.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.os4all.modules.user.entity.Role;
import org.os4all.modules.user.entity.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private final String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private final long expirationMs = 3600000; // 1 hour
    private final long refreshExpirationMs = 7200000; // 2 hours

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(secret, expirationMs, refreshExpirationMs);
    }

    @Test
    @DisplayName("Should generate valid JWT token and correctly extract claims")
    void shouldGenerateAndExtractToken() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setEmail("sarah@example.com");
        user.setPasswordHash("hashed_password");
        user.setRole(Role.ROLE_USER);
        user.setActive(true);

        UserDetails userDetails = new CustomUserDetails(user);

        String token = jwtService.generateToken(userDetails, userId);
        assertNotNull(token);
        assertFalse(token.isEmpty());

        String extractedUsername = jwtService.extractUsername(token);
        assertEquals("sarah@example.com", extractedUsername);

        UUID extractedUserId = jwtService.extractUserId(token);
        assertEquals(userId, extractedUserId);

        boolean isValid = jwtService.isTokenValid(token, userDetails);
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should invalidate token when user details mismatch")
    void shouldInvalidateTokenForDifferentUser() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setEmail("user1@example.com");
        user.setPasswordHash("hash");
        user.setRole(Role.ROLE_USER);
        user.setActive(true);

        UserDetails userDetails1 = new CustomUserDetails(user);
        String token = jwtService.generateToken(userDetails1, userId);

        User differentUser = new User();
        differentUser.setId(UUID.randomUUID());
        differentUser.setEmail("user2@example.com");
        differentUser.setPasswordHash("hash");
        differentUser.setRole(Role.ROLE_USER);
        differentUser.setActive(true);
        UserDetails userDetails2 = new CustomUserDetails(differentUser);

        assertFalse(jwtService.isTokenValid(token, userDetails2));
    }
}
