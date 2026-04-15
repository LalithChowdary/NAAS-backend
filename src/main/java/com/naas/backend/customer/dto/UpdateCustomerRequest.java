package com.naas.backend.customer.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateCustomerRequest {

    private String name;

    @Pattern(
        regexp = "^[6-9]\\d{9}$",
        message = "Phone number must be a valid 10-digit Indian mobile number starting with 6, 7, 8, or 9"
    )
    private String phone;
}
