package org.os4all.modules.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.os4all.core.exception.DuplicateResourceException;
import org.os4all.core.exception.UnauthorizedException;
import org.os4all.core.security.CustomUserDetails;
import org.os4all.core.security.JwtService;
import org.os4all.modules.audit.service.AuditService;
import org.os4all.modules.auth.dto.AuthResponse;
import org.os4all.modules.auth.dto.LoginRequest;
import org.os4all.modules.auth.dto.RegisterRequest;
import org.os4all.modules.auth.service.AuthService;
import org.os4all.modules.user.entity.Role;
import org.os4all.modules.user.entity.User;
import org.os4all.modules.user.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest("sarah@example.com", "Password123!", "Sarah Connor");
        loginRequest = new LoginRequest("sarah@example.com", "Password123!");
    }

    @Test
    @DisplayName("Should successfully register a new user")
    void shouldRegisterNewUser() {
        when(userRepository.existsByEmail("sarah@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("encoded_pass");

        User savedUser = new User();
        savedUser.setId(UUID.randomUUID());
        savedUser.setEmail("sarah@example.com");
        savedUser.setFullName("Sarah Connor");
        savedUser.setPasswordHash("encoded_pass");
        savedUser.setRole(Role.ROLE_USER);
        savedUser.setActive(true);

        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(any(), any())).thenReturn("mock_token");
        when(jwtService.generateRefreshToken(any(), any())).thenReturn("mock_refresh_token");

        AuthResponse response = authService.register(registerRequest, "127.0.0.1");

        assertNotNull(response);
        assertEquals("sarah@example.com", response.getEmail());
        assertEquals("mock_token", response.getToken());
        assertEquals("mock_refresh_token", response.getRefreshToken());
        assertEquals("Sarah Connor", response.getFullName());
        assertEquals("ROLE_USER", response.getRole());

        verify(userRepository).save(any(User.class));
        verify(auditService).record(any(), eq("sarah@example.com"), eq("USER_REGISTERED"), eq("USER"), any(), eq("127.0.0.1"), any());
    }

    @Test
    @DisplayName("Should reject registration if email is already taken")
    void shouldRejectDuplicateEmailRegistration() {
        when(userRepository.existsByEmail("sarah@example.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(registerRequest, "127.0.0.1"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should successfully authenticate user with correct credentials")
    void shouldLoginSuccessfully() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("sarah@example.com");
        user.setFullName("Sarah Connor");
        user.setPasswordHash("encoded_pass");
        user.setRole(Role.ROLE_USER);
        user.setActive(true);

        CustomUserDetails userDetails = new CustomUserDetails(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(userRepository.findByEmail("sarah@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(any(), any())).thenReturn("login_token");
        when(jwtService.generateRefreshToken(any(), any())).thenReturn("login_refresh_token");

        AuthResponse response = authService.login(loginRequest, "127.0.0.1");

        assertNotNull(response);
        assertEquals("login_token", response.getToken());
        assertEquals("sarah@example.com", response.getEmail());
        verify(auditService).record(any(), eq("sarah@example.com"), eq("USER_LOGIN_SUCCESS"), eq("AUTH"), any(), eq("127.0.0.1"), any());
    }

    @Test
    @DisplayName("Should reject login with bad credentials")
    void shouldRejectBadCredentials() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(UnauthorizedException.class, () -> authService.login(loginRequest, "127.0.0.1"));
        verify(auditService).record(isNull(), eq("sarah@example.com"), eq("USER_LOGIN_FAILED"), eq("AUTH"), isNull(), eq("127.0.0.1"), any());
    }
}
