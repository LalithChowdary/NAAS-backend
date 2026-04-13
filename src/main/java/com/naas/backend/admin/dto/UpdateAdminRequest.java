package com.naas.backend.admin.dto;

import lombok.Data;

@Data
public class UpdateAdminRequest {
    private String name;
    private String phone;
    private String employeeId;
}
