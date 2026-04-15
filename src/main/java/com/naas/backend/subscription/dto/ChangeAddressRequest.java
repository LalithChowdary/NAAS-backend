package com.naas.backend.subscription.dto;

import java.util.UUID;
import lombok.Data;

@Data
public class ChangeAddressRequest {
    private UUID addressId;
}
