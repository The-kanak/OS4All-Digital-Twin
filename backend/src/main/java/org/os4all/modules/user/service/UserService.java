package org.os4all.modules.user.service;

import org.os4all.core.exception.ResourceNotFoundException;
import org.os4all.modules.audit.service.AuditService;
import org.os4all.modules.user.dto.UpdateProfileRequest;
import org.os4all.modules.user.dto.UserProfileResponse;
import org.os4all.modules.user.entity.User;
import org.os4all.modules.user.entity.UserProfile;
import org.os4all.modules.user.repository.UserProfileRepository;
import org.os4all.modules.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final AuditService auditService;

    public UserService(UserRepository userRepository,
                       UserProfileRepository userProfileRepository,
                       AuditService auditService) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseGet(() -> new UserProfile(user));

        return mapToResponse(user, profile);
    }

    @Transactional
    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request, String ipAddress) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserProfile newProfile = new UserProfile(user);
                    return userProfileRepository.save(newProfile);
                });

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName().trim());
        }

        if (request.getDateOfBirth() != null) {
            profile.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getBiologicalSex() != null) {
            profile.setBiologicalSex(request.getBiologicalSex());
        }
        if (request.getHeightCm() != null) {
            profile.setHeightCm(request.getHeightCm());
        }
        if (request.getWeightKg() != null) {
            profile.setWeightKg(request.getWeightKg());
        }
        if (request.getBloodType() != null) {
            profile.setBloodType(request.getBloodType());
        }
        if (request.getLifestyleNotes() != null) {
            profile.setLifestyleNotes(request.getLifestyleNotes());
        }

        userRepository.save(user);
        UserProfile savedProfile = userProfileRepository.save(profile);

        auditService.record(user, user.getEmail(), "USER_PROFILE_UPDATED", "USER_PROFILE", savedProfile.getId().toString(), ipAddress, "Profile metrics updated");

        log.info("Profile updated for user id: {}", userId);
        return mapToResponse(user, savedProfile);
    }

    private UserProfileResponse mapToResponse(User user, UserProfile profile) {
        UserProfileResponse response = new UserProfileResponse();
        response.setUserId(user.getId());
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setRole(user.getRole().name());

        if (profile != null) {
            response.setDateOfBirth(profile.getDateOfBirth());
            response.setBiologicalSex(profile.getBiologicalSex());
            response.setHeightCm(profile.getHeightCm());
            response.setWeightKg(profile.getWeightKg());
            response.setBloodType(profile.getBloodType());
            response.setLifestyleNotes(profile.getLifestyleNotes());
        }

        return response;
    }
}
