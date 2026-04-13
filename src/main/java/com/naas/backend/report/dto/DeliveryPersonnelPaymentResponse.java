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
public class DeliveryPersonnelPaymentResponse {
    private UUID deliveryPersonId;
    private String deliveryPersonName;
    private String employeeId;
    private long deliveriesCompleted;
    private BigDecimal totalDeliveryValue;
    private BigDecimal paymentAmount; // 2.5% of value
}
