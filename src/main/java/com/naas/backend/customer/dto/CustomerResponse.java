package com.naas.backend.customer.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CustomerResponse {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private boolean active;
    private LocalDateTime createdAt;
}
