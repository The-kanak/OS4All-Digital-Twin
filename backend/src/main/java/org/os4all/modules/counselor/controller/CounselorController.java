package org.os4all.modules.counselor.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.os4all.core.common.ApiResponse;
import org.os4all.core.exception.UnauthorizedException;
import org.os4all.core.security.CustomUserDetails;
import org.os4all.modules.counselor.dto.*;
import org.os4all.modules.counselor.service.CounselorService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/counselor")
@Tag(name = "OS4All AI Counselor", description = "Conversational interface over structured health context, baselines, trends, labs, and evidence")
@SecurityRequirement(name = "BearerAuth")
public class CounselorController {

    private final CounselorService counselorService;

    public CounselorController(CounselorService counselorService) {
        this.counselorService = counselorService;
    }

    @PostMapping("/chat")
    @Operation(
            summary = "Send a message to the AI Health Counselor",
            description = "Interacts with the conversational counselor, strictly grounded in the user's structured health baselines, trends, reports, and evidence. Provides clear distinctions between observations, interpretations, evidence, and actions."
    )
    public ResponseEntity<ApiResponse<CounselorResponseDto>> chat(
            @Valid @RequestBody CounselorChatRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest httpRequest
    ) {
        verifyAuthenticated(userDetails);
        String clientIp = httpRequest.getRemoteAddr();
        CounselorResponseDto response = counselorService.chat(userDetails.getId(), request, clientIp);
        return ResponseEntity.ok(ApiResponse.success("Counselor response generated successfully.", response));
    }

    @GetMapping("/conversations")
    @Operation(
            summary = "List counselor conversation threads",
            description = "Retrieves user's stored counselor conversation threads with message counts and timestamps."
    )
    public ResponseEntity<ApiResponse<Page<CounselorConversationSummaryDto>>> listConversations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        verifyAuthenticated(userDetails);
        Page<CounselorConversationSummaryDto> conversations = counselorService.listConversations(
                userDetails.getId(),
                PageRequest.of(page, size)
        );
        return ResponseEntity.ok(ApiResponse.success(conversations));
    }

    @GetMapping("/conversations/{id}")
    @Operation(
            summary = "Get conversation details and full message history",
            description = "Retrieves a specific counselor conversation thread along with all user and assistant messages and structured payloads."
    )
    public ResponseEntity<ApiResponse<CounselorConversationDetailDto>> getConversation(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        verifyAuthenticated(userDetails);
        CounselorConversationDetailDto detail = counselorService.getConversation(id, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success(detail));
    }

    @DeleteMapping("/conversations/{id}")
    @Operation(
            summary = "Delete a conversation thread",
            description = "Securely deletes a counselor conversation thread and its associated message history."
    )
    public ResponseEntity<ApiResponse<Void>> deleteConversation(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        verifyAuthenticated(userDetails);
        counselorService.deleteConversation(id, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Conversation deleted successfully.", null));
    }

    private void verifyAuthenticated(CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new UnauthorizedException("User is not authenticated");
        }
    }
}
