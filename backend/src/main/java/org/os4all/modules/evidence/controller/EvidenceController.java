package org.os4all.modules.evidence.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.os4all.core.common.ApiResponse;
import org.os4all.core.exception.UnauthorizedException;
import org.os4all.core.security.CustomUserDetails;
import org.os4all.modules.evidence.model.EvidenceQuery;
import org.os4all.modules.evidence.model.EvidenceResult;
import org.os4all.modules.evidence.service.EvidenceService;
import org.os4all.modules.evidence.service.EvidenceServiceRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/evidence")
@Tag(name = "Evidence Retrieval", description = "Endpoints for querying biomedical evidence sources via Tavily and offline biomedical literature databases")
@SecurityRequirement(name = "BearerAuth")
public class EvidenceController {

    private final EvidenceServiceRegistry evidenceRegistry;

    public EvidenceController(EvidenceServiceRegistry evidenceRegistry) {
        this.evidenceRegistry = evidenceRegistry;
    }

    @GetMapping("/search")
    @Operation(
            summary = "Query scientific evidence for a health signal or query text",
            description = "Performs constrained search against authoritative medical and scientific literature domains (e.g. PubMed, NIH, Nature Medicine) via Tavily/Mock provider."
    )
    public ResponseEntity<ApiResponse<EvidenceResult>> searchEvidence(
            @RequestParam String query,
            @RequestParam(defaultValue = "3") int maxResults,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        verifyAuthenticated(userDetails);
        EvidenceService activeService = evidenceRegistry.getActiveService();
        if (activeService == null) {
            return ResponseEntity.ok(ApiResponse.success("Evidence retrieval is currently disabled.",
                    new EvidenceResult(new EvidenceQuery(query, List.of(), "", true, maxResults), List.of(), false, "none", 0)));
        }

        EvidenceQuery evidenceQuery = new EvidenceQuery(
                query,
                List.of(),
                "User requested query",
                true,
                maxResults
        );

        EvidenceResult result = activeService.retrieveEvidence(evidenceQuery);
        return ResponseEntity.ok(ApiResponse.success("Evidence retrieved successfully", result));
    }

    private void verifyAuthenticated(CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new UnauthorizedException("User is not authenticated");
        }
    }
}
