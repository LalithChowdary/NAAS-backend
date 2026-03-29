package com.naas.backend.billing.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class BillItemResponseDTO {
    private Long id;
    private String publicationName;
    private int deliveriesCount;
    private BigDecimal pricePerUnit;
    private BigDecimal itemAmount;
}
