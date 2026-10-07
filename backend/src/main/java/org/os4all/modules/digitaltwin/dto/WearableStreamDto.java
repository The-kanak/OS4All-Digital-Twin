package org.os4all.modules.digitaltwin.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record WearableStreamDto(
        UUID patientId,
        String metric,
        String unit,
        List<DataPointDto> points,
        BigDecimal baselineMean,
        BigDecimal baselineMin,
        BigDecimal baselineMax,
        Map<String, Object> summaryStatistics
) {
    public record DataPointDto(
            BigDecimal value,
            String timestampIso,
            String source,
            BigDecimal confidence
    ) {}
}
