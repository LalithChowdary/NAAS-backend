package com.naas.backend.delivery.dto;

import java.util.UUID;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class DeliveryScheduleResponse {
    private UUID customerId;
    private String customerName;
    private String customerAddress;
    private String customerPhone;
    private List<PublicationDelivery> publications;

    @Data
    @Builder
    public static class PublicationDelivery {
        private UUID subscriptionId;
        private UUID publicationId;
        private String publicationName;
        private String status; // PENDING, DELIVERED, CANCELLED
    }
}
