package com.naas.backend.deliveryperson.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DeliveryPersonSignupRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @Pattern(
        regexp = "^[6-9]\\d{9}$",
        message = "Phone number must be a valid 10-digit Indian mobile number starting with 6, 7, 8, or 9"
    )
    private String phone;
}
