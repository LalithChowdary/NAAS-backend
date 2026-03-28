package com.naas.backend.admin;

import com.naas.backend.admin.dto.CreateAdminRequest;
import com.naas.backend.admin.dto.CreateDeliveryPersonRequest;
import com.naas.backend.customer.CustomerService;
import com.naas.backend.customer.dto.CreateCustomerByAdminRequest;
import com.naas.backend.customer.dto.CustomerResponse;
import com.naas.backend.customer.dto.UpdateCustomerRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final CustomerService customerService;

    // ----------------------------------------------------------------
    // Existing: create admin / delivery person
    // ----------------------------------------------------------------

    @PostMapping("/create-admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> createAdmin(@Valid @RequestBody CreateAdminRequest request) {
        return ResponseEntity.ok(adminService.createAdmin(request));
    }

    @PostMapping("/create-delivery-person")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> createDeliveryPerson(@Valid @RequestBody CreateDeliveryPersonRequest request) {
        return ResponseEntity.ok(adminService.createDeliveryPerson(request));
    }

    // ----------------------------------------------------------------
    // FR-CM4: List / search customers
    // ----------------------------------------------------------------

    @GetMapping("/customers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CustomerResponse>> getCustomers(
            @RequestParam(required = false) String search) {
        if (search != null && !search.isBlank()) {
            return ResponseEntity.ok(customerService.searchCustomers(search));
        }
        return ResponseEntity.ok(customerService.getAllCustomers());
    }

    // FR-CM3: View single customer
    @GetMapping("/customers/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CustomerResponse> getCustomer(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.getCustomerById(id));
    }

    // FR-CM1: Create customer
    @PostMapping("/customers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CustomerResponse> createCustomer(
            @Valid @RequestBody CreateCustomerByAdminRequest request) {
        return ResponseEntity.ok(customerService.createCustomer(request));
    }

    // FR-CM2: Update customer
    @PutMapping("/customers/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable Long id,
            @RequestBody UpdateCustomerRequest request) {
        return ResponseEntity.ok(customerService.updateCustomer(id, request));
    }

    // FR-CM5: Toggle active/inactive status
    @PatchMapping("/customers/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CustomerResponse> toggleCustomerStatus(
            @PathVariable Long id,
            @RequestParam boolean active) {
        return ResponseEntity.ok(customerService.toggleStatus(id, active));
    }

    // FR-CM6: Soft-delete customer
    @DeleteMapping("/customers/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteCustomer(@PathVariable Long id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.ok("Customer deactivated successfully");
    }
}
