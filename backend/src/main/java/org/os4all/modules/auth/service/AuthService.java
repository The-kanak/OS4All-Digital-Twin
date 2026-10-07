package org.os4all.modules.auth.service;

import org.os4all.core.exception.DuplicateResourceException;
import org.os4all.core.exception.UnauthorizedException;
import org.os4all.core.security.CustomUserDetails;
import org.os4all.core.security.JwtService;
import org.os4all.modules.audit.service.AuditService;
import org.os4all.modules.auth.dto.AuthResponse;
import org.os4all.modules.auth.dto.LoginRequest;
import org.os4all.modules.auth.dto.RegisterRequest;
import org.os4all.modules.user.entity.Role;
import org.os4all.modules.user.entity.User;
import org.os4all.modules.user.entity.UserProfile;
import org.os4all.modules.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final AuditService auditService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager,
                       AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.auditService = auditService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request, String ipAddress) {
        log.info("Attempting to register new user: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User already exists with email: " + request.getEmail());
        }

        User user = new User();
        user.setEmail(request.getEmail().toLowerCase().trim());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName().trim());
        user.setRole(Role.ROLE_USER);
        user.setActive(true);

        UserProfile profile = new UserProfile(user);
        user.setProfile(profile);

        User savedUser = userRepository.save(user);

        CustomUserDetails userDetails = new CustomUserDetails(savedUser);
        String token = jwtService.generateToken(userDetails, savedUser.getId());
        String refreshToken = jwtService.generateRefreshToken(userDetails, savedUser.getId());

        auditService.record(savedUser, savedUser.getEmail(), "USER_REGISTERED", "USER", savedUser.getId().toString(), ipAddress, "New user registration");

        log.info("User registered successfully with id: {}", savedUser.getId());

        return new AuthResponse(token, refreshToken, savedUser.getId(), savedUser.getEmail(), savedUser.getFullName(), savedUser.getRole().name());
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress) {
        String email = request.getEmail().toLowerCase().trim();
        log.info("Authenticating user: {}", email);

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword())
            );

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UnauthorizedException("User not found"));

            String token = jwtService.generateToken(userDetails, user.getId());
            String refreshToken = jwtService.generateRefreshToken(userDetails, user.getId());

            auditService.record(user, user.getEmail(), "USER_LOGIN_SUCCESS", "AUTH", user.getId().toString(), ipAddress, "Successful login");

            return new AuthResponse(token, refreshToken, user.getId(), user.getEmail(), user.getFullName(), user.getRole().name());
        } catch (BadCredentialsException e) {
            auditService.record(null, email, "USER_LOGIN_FAILED", "AUTH", null, ipAddress, "Bad credentials");
            throw new UnauthorizedException("Invalid email or password");
        }
    }
}
