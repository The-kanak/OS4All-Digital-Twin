package org.os4all.modules.digitaltwin.dto;

import java.util.List;
import java.util.UUID;

public record GeminiExplanationResponseDto(
        UUID patientId,
        String explanation,
        List<String> keyFactors,
        String trajectoryExplanation,
        String source, // "GEMINI" or "FALLBACK"
        String model,  // e.g. "gemini-1.5-flash" or "deterministic-offline-engine"
        String generatedAt,
        String disclaimer
) {}
