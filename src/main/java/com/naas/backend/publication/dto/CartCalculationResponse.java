package com.naas.backend.publication.dto;

import java.util.List;
import lombok.Data;

@Data
public class CartCalculationResponse {
    private List<CartItemResponse> items;
    private Double totalMonthlyCost;
}
