package org.os4all.modules.digitaltwin.dto;

public record GeminiChatRequestDto(
        String question
) {
    public GeminiChatRequestDto() {
        this("");
    }
}
