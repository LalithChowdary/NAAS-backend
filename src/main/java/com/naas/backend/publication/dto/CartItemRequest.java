package com.naas.backend.publication.dto;

import java.util.UUID;
import lombok.Data;

@Data
public class CartItemRequest {
    private UUID publicationId;
    private int quantity;
}
