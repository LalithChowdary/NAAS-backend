package com.naas.backend.customer.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CustomerAddressResponse {
    private Long id;
    private Long customerId;
    private String label;
    private String address;
    private Double latitude;
    private Double longitude;
    private String house;
    private String area;
    private String landmark;
}
