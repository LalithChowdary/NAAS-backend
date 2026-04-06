package com.naas.backend.subscription.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CreateSubscriptionRequest {
    private List<ItemRequest> items;
    private LocalDate startDate;
    private Long addressId;

    @Data
    public static class ItemRequest {
        private Long publicationId;
        private String frequency;
        private String customDeliveryDays;
    }
}
