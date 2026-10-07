package org.os4all.modules.digitaltwin.dto;

public record GeminiExplanationRequestDto(
        String context, // e.g. "digital-twin"
        String focus    // e.g. "trajectory", "risk", "baseline", or null
) {
    public GeminiExplanationRequestDto() {
        this("digital-twin", null);
    }
}
