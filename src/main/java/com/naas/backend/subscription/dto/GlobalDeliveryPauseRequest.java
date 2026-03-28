package com.naas.backend.subscription.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class GlobalDeliveryPauseRequest {
    private LocalDate startDate;
    private LocalDate endDate;
}
