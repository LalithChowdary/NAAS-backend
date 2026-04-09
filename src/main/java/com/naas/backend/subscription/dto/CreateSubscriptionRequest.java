package com.naas.backend.subscription.dto;

import java.util.UUID;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CreateSubscriptionRequest {
    private List<ItemRequest> items;
    private LocalDate startDate;
    private UUID addressId;

    @Data
    public static class ItemRequest {
        private UUID publicationId;
        private String frequency;
        private String customDeliveryDays;
    }
}
