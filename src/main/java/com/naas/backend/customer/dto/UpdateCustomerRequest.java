package com.naas.backend.customer.dto;

import lombok.Data;

@Data
public class UpdateCustomerRequest {

    private String name;
    private String phone;
    private String address;
    private String city;
    private String pincode;
    private String area;
}
