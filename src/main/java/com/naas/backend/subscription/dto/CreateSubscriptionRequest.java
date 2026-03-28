package com.naas.backend.subscription.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CreateSubscriptionRequest {
    private List<Long> publicationIds;
    private LocalDate startDate;
}

