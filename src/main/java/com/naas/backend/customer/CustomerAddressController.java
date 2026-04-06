package com.naas.backend.customer;

import com.naas.backend.auth.entity.User;
import com.naas.backend.customer.dto.CreateCustomerAddressRequest;
import com.naas.backend.customer.dto.CustomerAddressResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customer/addresses")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerAddressController {

    private final CustomerAddressService customerAddressService;

    @GetMapping
    public ResponseEntity<List<CustomerAddressResponse>> getCustomerAddresses(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(customerAddressService.getCustomerAddresses(user));
    }

    @PostMapping
    public ResponseEntity<CustomerAddressResponse> addCustomerAddress(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateCustomerAddressRequest request) {
        return ResponseEntity.ok(customerAddressService.addCustomerAddress(user, request));
    }
}
