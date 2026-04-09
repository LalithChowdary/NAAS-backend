package com.naas.backend.subscription.dto;

import com.naas.backend.subscription.SubscriptionItemStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemStatusRequest {
    private SubscriptionItemStatus status;
    private LocalDate stopStartDate;
    private LocalDate stopEndDate;
}
