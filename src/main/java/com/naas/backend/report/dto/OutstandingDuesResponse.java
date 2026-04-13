package com.naas.backend.report.dto;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutstandingDuesResponse {
    private String customerName;
    private String phoneNumber;
    private String email;
    private BigDecimal totalDue;
    private int pendingBillsCount;
}