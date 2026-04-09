package com.naas.backend.delivery.dto;

import java.util.UUID;

import com.naas.backend.delivery.entity.DeliveryRecord;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class CustomerDeliveryResponse {
    private UUID id;
    private LocalDate deliveryDate;
    private String status;
    private String publicationName;
    private Double dailyCost;
}