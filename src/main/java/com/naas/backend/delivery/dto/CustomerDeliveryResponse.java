package com.naas.backend.delivery.dto;

import com.naas.backend.delivery.entity.DeliveryRecord;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class CustomerDeliveryResponse {
    private Long id;
    private LocalDate deliveryDate;
    private String status;
    private String publicationName;
    private Double dailyCost;
}