package org.os4all.modules.counselor.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(description = "Detailed counselor conversation with message history")
public record CounselorConversationDetailDto(
        UUID id,
        String title,
        String createdAtIso,
        String updatedAtIso,
        List<CounselorMessageItemDto> messages
) {
    public record CounselorMessageItemDto(
            UUID id,
            String role,
            int sequenceNumber,
            String content,
            CounselorResponseDto structuredData,
            boolean isEmergencyFlagged,
            String createdAtIso
    ) {}
}
