package com.naas.backend.customer.dto;

import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CustomerAddressResponse {
    private UUID id;
    private UUID customerId;
    private String label;
    private String address;
    private Double latitude;
    private Double longitude;
    private String house;
    private String area;
    private String landmark;
}
