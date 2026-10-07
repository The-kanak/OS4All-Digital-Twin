package org.os4all.modules.counselor.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(description = "Summary of a counselor conversation thread")
public record CounselorConversationSummaryDto(
        UUID id,
        String title,
        int messageCount,
        String createdAtIso,
        String updatedAtIso
) {}
