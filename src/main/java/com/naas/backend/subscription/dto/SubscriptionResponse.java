package com.naas.backend.subscription.dto;

import com.naas.backend.subscription.SubscriptionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class SubscriptionResponse {
    private Long id;
    private Long customerId;
    private String customerName;
    private Long publicationId; // leaving for backwards compatibility temporarily
    private String publicationName;
    private List<SubscriptionItemResponse> items; // The new proper schema
    private SubscriptionStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate suspendStartDate;
    private LocalDate suspendEndDate;
    private LocalDateTime createdAt;
}
