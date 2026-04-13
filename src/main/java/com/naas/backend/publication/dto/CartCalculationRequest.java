package com.naas.backend.publication.dto;

import java.util.List;
import lombok.Data;

@Data
public class CartCalculationRequest {
    private List<CartItemRequest> items;
}
