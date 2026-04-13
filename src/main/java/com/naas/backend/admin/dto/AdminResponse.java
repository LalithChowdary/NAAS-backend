package com.naas.backend.admin.dto;

import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminResponse {
    private UUID id;
    private String name;
    private String email;
    private String phone;
    private String employeeId;
    private boolean active;
}
