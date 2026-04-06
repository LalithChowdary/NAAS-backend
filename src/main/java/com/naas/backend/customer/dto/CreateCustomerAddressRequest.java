package com.naas.backend.customer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateCustomerAddressRequest {

    @NotBlank(message = "Label is required")
    private String label;

    @NotBlank(message = "Address is required")
    private String address;

    private Double latitude;
    private Double longitude;

    private String house;
    private String area;
    private String landmark;
}
