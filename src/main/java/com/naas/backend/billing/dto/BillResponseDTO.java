package com.naas.backend.billing.dto;

import java.util.UUID;

import com.naas.backend.billing.entity.BillStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class BillResponseDTO {
    private UUID id;
    private UUID customerId;
    private String customerName;
    private String customerAddress;
    private String billingMonth;
    private BigDecimal totalAmount;
    private LocalDate dueDate;
    private BillStatus status;
    private LocalDateTime createdAt;
    private List<BillItemResponseDTO> items;
}
