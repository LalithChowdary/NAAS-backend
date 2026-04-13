package com.naas.backend.report.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlySummaryResponse {
    private String month; // e.g. "04-2026"
    private long totalActiveSubscriptions;
    private BigDecimal totalBilled;
    private BigDecimal totalCollected;
    private long totalDeliveries;
    private long successfulDeliveries;
}