package com.naas.backend.customer.dto;

import java.util.UUID;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CustomerResponse {

    private UUID id;
    private String name;
    private String email;
    private String phone;
    private boolean active;
    private LocalDateTime createdAt;
}
