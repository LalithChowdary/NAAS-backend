package com.naas.backend.subscription.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class SuspendSubscriptionRequest {
    private LocalDate suspendStartDate;
    private LocalDate suspendEndDate;
}
