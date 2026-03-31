package com.naas.backend.billing.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentResponseDTO {
    private Long id;
    private Long billId;
    private String billingMonth;
    private Long customerId;
    private String customerName;
    private BigDecimal amount;
    private String paymentMethod;
    private String chequeNumber;
    private String receiptNote;
    private LocalDateTime paidAt;
}
