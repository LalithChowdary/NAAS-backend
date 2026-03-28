package com.naas.backend.delivery.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class DeliveryScheduleResponse {
    private Long customerId;
    private String customerName;
    private String customerAddress;
    private String customerPhone;
    private List<PublicationDelivery> publications;

    @Data
    @Builder
    public static class PublicationDelivery {
        private Long subscriptionId;
        private Long publicationId;
        private String publicationName;
        private String status; // PENDING, DELIVERED, CANCELLED
    }
}
