package com.naas.backend.subscription.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubscriptionItemResponse {
    private Long id;
    private Long publicationId;
    private String publicationName;
    private double price;
    private String type; // Magazine vs Newspaper
    private String frequency;
    private String customDeliveryDays;
}
