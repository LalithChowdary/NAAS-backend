package com.naas.backend.deliveryperson.dto;

import com.naas.backend.deliveryperson.DeliveryPersonPayout.PayoutStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class PayoutResponse {
    private UUID id;
    private UUID deliveryPersonId;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal amountPaid;
    private LocalDateTime paymentDate;
    private PayoutStatus status;
}
