package com.naas.backend.report.dto;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliverySummaryResponse {
    private LocalDate date;
    private long totalAssigned;
    private long delivered;
    private long cancelled;
    private long pending;
}
