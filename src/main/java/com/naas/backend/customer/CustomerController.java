package com.naas.backend.customer;

import com.naas.backend.auth.entity.User;
import com.naas.backend.customer.dto.CustomerResponse;
import com.naas.backend.customer.dto.UpdateCustomerRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    // FR-CM3: Customer views own profile
    @GetMapping("/profile")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CustomerResponse> getProfile(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(customerService.getProfile(user));
    }

    // FR-CM2: Customer updates own profile
    @PutMapping("/profile")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CustomerResponse> updateProfile(
            @AuthenticationPrincipal User user,
            @RequestBody UpdateCustomerRequest request) {
        return ResponseEntity.ok(customerService.updateProfile(user, request));
    }

    // FR-CM6: Customer deactivates own account
    @DeleteMapping("/account")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<String> deleteAccount(@AuthenticationPrincipal User user) {
        customerService.deactivateAccount(user);
        return ResponseEntity.ok("Account deactivated successfully");
    }
}
