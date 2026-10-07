package org.os4all.modules.ai.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.os4all.core.common.ApiResponse;
import org.os4all.core.exception.UnauthorizedException;
import org.os4all.core.security.CustomUserDetails;
import org.os4all.modules.ai.entity.AiInferenceLog;
import org.os4all.modules.ai.model.AiHealthInsightResponse;
import org.os4all.modules.ai.service.AIOrchestrator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai")
@Tag(name = "AI Intelligence Layer", description = "AI Orchestration endpoints with provider abstraction, multi-agent workflows, and non-diagnostic health synthesis")
@SecurityRequirement(name = "BearerAuth")
public class AIController {

    private final AIOrchestrator aiOrchestrator;

    public AIController(AIOrchestrator aiOrchestrator) {
        this.aiOrchestrator = aiOrchestrator;
    }

    @PostMapping("/insights/evaluate")
    @Operation(
            summary = "Trigger multi-agent AI health evaluation",
            description = "Runs the Health Context -> Trend Interpretation -> Evidence -> Explanation -> Action workflow to produce structured, non-diagnostic health interpretations strictly grounded in individual baselines."
    )
    public ResponseEntity<ApiResponse<AiHealthInsightResponse>> evaluateHealthInsight(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest request
    ) {
        verifyAuthenticated(userDetails);
        String clientIp = request.getRemoteAddr();
        AiHealthInsightResponse response = aiOrchestrator.generateHealthInsight(userDetails.getId(), clientIp);
        return ResponseEntity.ok(ApiResponse.success("AI health evaluation completed successfully.", response));
    }

    @GetMapping("/insights/logs")
    @Operation(
            summary = "Get user AI inference audit logs",
            description = "Retrieves timestamped AI inferences, anonymized prompts, execution latencies, and structured output summaries."
    )
    public ResponseEntity<ApiResponse<Page<AiInferenceLog>>> getInferenceLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        verifyAuthenticated(userDetails);
        Page<AiInferenceLog> logs = aiOrchestrator.getUserInferenceLogs(userDetails.getId(), PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    private void verifyAuthenticated(CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new UnauthorizedException("User is not authenticated");
        }
    }
}
