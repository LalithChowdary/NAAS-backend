package com.naas.backend.subscription.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class GlobalDeliveryPauseResponse {
    private Long id;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime createdAt;
}
