package org.os4all.modules.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.os4all.core.common.ApiResponse;
import org.os4all.core.exception.UnauthorizedException;
import org.os4all.core.security.CustomUserDetails;
import org.os4all.modules.consent.dto.ConsentRequest;
import org.os4all.modules.consent.dto.ConsentResponse;
import org.os4all.modules.consent.service.ConsentService;
import org.os4all.modules.user.dto.UpdateProfileRequest;
import org.os4all.modules.user.dto.UserProfileResponse;
import org.os4all.modules.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User & Profile", description = "Endpoints for user profile and granular privacy consent management")
@SecurityRequirement(name = "BearerAuth")
public class UserController {

    private final UserService userService;
    private final ConsentService consentService;

    public UserController(UserService userService, ConsentService consentService) {
        this.userService = userService;
        this.consentService = consentService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user profile", description = "Retrieves the authenticated user's demographics and clinical profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        verifyAuthenticated(userDetails);
        UserProfileResponse response = userService.getProfile(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user profile", description = "Updates the authenticated user's demographics, biometric attributes, and lifestyle tags")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request,
            HttpServletRequest httpRequest) {
        verifyAuthenticated(userDetails);
        String clientIp = getClientIp(httpRequest);
        UserProfileResponse response = userService.updateProfile(userDetails.getId(), request, clientIp);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", response));
    }

    @PostMapping("/consent")
    @Operation(summary = "Grant or revoke consent", description = "Updates a granular privacy consent item (DATA_STORAGE, AI_INFERENCE, EVIDENCE_SEARCH, RESEARCH)")
    public ResponseEntity<ApiResponse<ConsentResponse>> updateConsent(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ConsentRequest request,
            HttpServletRequest httpRequest) {
        verifyAuthenticated(userDetails);
        String clientIp = getClientIp(httpRequest);
        ConsentResponse response = consentService.updateConsent(userDetails.getId(), request, clientIp);
        return ResponseEntity.ok(ApiResponse.success("Consent updated successfully", response));
    }

    @GetMapping("/consent")
    @Operation(summary = "Get user consents", description = "Retrieves all active and revoked consents for the authenticated user")
    public ResponseEntity<ApiResponse<List<ConsentResponse>>> getConsents(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        verifyAuthenticated(userDetails);
        List<ConsentResponse> response = consentService.getUserConsents(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private void verifyAuthenticated(CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new UnauthorizedException("User is not authenticated");
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
