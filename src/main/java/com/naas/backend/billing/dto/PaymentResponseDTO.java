package com.naas.backend.billing.dto;

import java.util.UUID;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentResponseDTO {
    private UUID id;
    private UUID billId;
    private String billingMonth;
    private UUID customerId;
    private String customerName;
    private BigDecimal amount;
    private String paymentMethod;
    private String chequeNumber;
    private String receiptNote;
    private LocalDateTime paidAt;
}
