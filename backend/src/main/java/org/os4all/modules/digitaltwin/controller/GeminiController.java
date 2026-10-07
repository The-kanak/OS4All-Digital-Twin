package org.os4all.modules.digitaltwin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.os4all.core.common.ApiResponse;
import org.os4all.modules.digitaltwin.dto.*;
import org.os4all.modules.digitaltwin.service.DigitalTwinService;
import org.os4all.modules.digitaltwin.service.GeminiExplanationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Controller exposing grounded Gemini AI Clinical Explanation and Virtual Patient conversational endpoints.
 * All operations operate strictly over authoritative backend Digital Twin state.
 */
@RestController
@RequestMapping("/api/v1/patients/{patientId}/gemini")
@Tag(name = "Gemini Digital Twin Intelligence", description = "Endpoints for grounded clinical explanations and virtual patient conversations via Gemini AI")
public class GeminiController {

    private final DigitalTwinService digitalTwinService;
    private final GeminiExplanationService geminiService;

    public GeminiController(
            DigitalTwinService digitalTwinService,
            GeminiExplanationService geminiService
    ) {
        this.digitalTwinService = digitalTwinService;
        this.geminiService = geminiService;
    }

    @PostMapping("/explanation")
    @Operation(summary = "Generate Grounded AI Clinical Explanation", description = "Generates a clinician-facing explanation strictly grounded in authoritative Digital Twin state and 2-hour trajectory projection.")
    public ResponseEntity<ApiResponse<GeminiExplanationResponseDto>> getGeminiExplanation(
            @PathVariable UUID patientId,
            @RequestBody(required = false) GeminiExplanationRequestDto request
    ) {
        PatientDetailDto patient = digitalTwinService.getPatientDetail(patientId);
        DigitalTwinStateDto state = digitalTwinService.getDigitalTwinState(patientId);
        PredictionResultDto pred = digitalTwinService.getLatestPrediction(patientId);

        String focus = request != null ? request.focus() : null;
        GeminiExplanationResponseDto explanation = geminiService.generateExplanation(patient, state, pred, focus);

        return ResponseEntity.ok(ApiResponse.success("AI clinical explanation generated successfully.", explanation));
    }

    @PostMapping("/chat")
    @Operation(summary = "Chat with Virtual Patient Digital Twin via Gemini", description = "Ask clinical questions answered strictly by grounding over verified Digital Twin telemetry.")
    public ResponseEntity<ApiResponse<GeminiChatResponseDto>> chatWithGemini(
            @PathVariable UUID patientId,
            @RequestBody(required = false) GeminiChatRequestDto request
    ) {
        PatientDetailDto patient = digitalTwinService.getPatientDetail(patientId);
        DigitalTwinStateDto state = digitalTwinService.getDigitalTwinState(patientId);
        PredictionResultDto pred = digitalTwinService.getLatestPrediction(patientId);

        String question = (request != null && request.question() != null && !request.question().isBlank())
                ? request.question()
                : "What is the patient's current metabolic risk and trajectory?";

        GeminiChatResponseDto response = geminiService.chatGrounded(patient, state, pred, question);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/status")
    @Operation(summary = "Get Gemini Service Status", description = "Reports whether Gemini AI is configured and active, without exposing any secrets.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getGeminiStatus(@PathVariable UUID patientId) {
        boolean configured = geminiService.isGeminiConfigured();
        Map<String, Object> status = Map.of(
                "configured", configured,
                "provider", "Official Google GenAI SDK (com.google.genai.Client)",
                "model", geminiService.getModelName(),
                "mode", configured ? "LIVE_GEMINI_AI" : "DETERMINISTIC_OFFLINE_FALLBACK"
        );
        return ResponseEntity.ok(ApiResponse.success(status));
    }
}
