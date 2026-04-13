package com.naas.backend.publication.dto;

import java.util.UUID;
import lombok.Data;

@Data
public class CartItemResponse {
    private UUID publicationId;
    private String name;
    private String frequency;
    private Double pricePerIssue;
    private Double monthlyCost;
    private int quantity;
}
