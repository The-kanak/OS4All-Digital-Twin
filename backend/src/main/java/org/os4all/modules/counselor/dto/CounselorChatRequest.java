package org.os4all.modules.counselor.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(description = "Request message sent to the OS4All AI Health Counselor")
public record CounselorChatRequest(
        @Schema(description = "Existing conversation ID. If null, a new conversation is initiated.")
        UUID conversationId,

        @NotBlank(message = "Message cannot be empty")
        @Size(max = 2000, message = "Message must not exceed 2000 characters")
        @Schema(description = "User query or message content", example = "Why did my health status change?")
        String message
) {}
