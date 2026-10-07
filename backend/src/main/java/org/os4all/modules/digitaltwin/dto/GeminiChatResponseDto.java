package org.os4all.modules.digitaltwin.dto;

import java.util.List;
import java.util.UUID;

public record GeminiChatResponseDto(
        UUID patientId,
        String question,
        String answer,
        List<String> keyFactors,
        String source, // "GEMINI" or "FALLBACK"
        String model,
        String generatedAt,
        String disclaimer
) {}
