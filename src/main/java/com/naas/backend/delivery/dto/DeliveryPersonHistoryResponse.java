package com.naas.backend.delivery.dto;

import java.util.UUID;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class DeliveryPersonHistoryResponse {
    private UUID id;
    private UUID subscriptionId;
    private LocalDate deliveryDate;
    private String status;
    private String publications;
    private String customerName;
    private String customerAddress;
    private Double totalValue;
    private Double payout;
}
