package com.naas.backend.deliveryperson.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PayoutRequest {
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal amountPaid;
}
