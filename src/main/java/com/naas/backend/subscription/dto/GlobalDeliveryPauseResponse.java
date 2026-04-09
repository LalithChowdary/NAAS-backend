package com.naas.backend.subscription.dto;

import java.util.UUID;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class GlobalDeliveryPauseResponse {
    private UUID id;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime createdAt;
}
