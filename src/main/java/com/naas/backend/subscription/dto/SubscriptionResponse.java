package com.naas.backend.subscription.dto;

import com.naas.backend.subscription.SubscriptionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class SubscriptionResponse {
    private Long id;
    private Long publicationId;
    private String publicationName;
    private SubscriptionStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate suspendStartDate;
    private LocalDate suspendEndDate;
    private LocalDateTime createdAt;
}
