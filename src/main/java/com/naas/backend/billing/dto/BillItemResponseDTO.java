package com.naas.backend.billing.dto;

import java.util.UUID;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class BillItemResponseDTO {
    private UUID id;
    private String publicationName;
    private int deliveriesCount;
    private BigDecimal pricePerUnit;
    private BigDecimal itemAmount;
}
