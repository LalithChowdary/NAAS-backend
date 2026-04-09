package com.naas.backend.subscription.dto;

import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubscriptionItemResponse {
    private UUID id;
    private UUID publicationId;
    private String publicationName;
    private double price;
    private String type; // Magazine vs Newspaper
    private String frequency;
    private String customDeliveryDays;
}
