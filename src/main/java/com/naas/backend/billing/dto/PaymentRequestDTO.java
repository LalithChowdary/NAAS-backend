package com.naas.backend.billing.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentRequestDTO {
    private BigDecimal amount;
    private String paymentMethod; // "CASH" or "CHEQUE"
    private String chequeNumber;
    private String receiptNote;
}
