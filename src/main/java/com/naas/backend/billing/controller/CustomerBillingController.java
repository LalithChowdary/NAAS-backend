package com.naas.backend.billing.controller;

import com.naas.backend.auth.entity.User;
import com.naas.backend.billing.dto.BillResponseDTO;
import com.naas.backend.billing.service.BillingService;
import com.naas.backend.customer.Customer;
import com.naas.backend.customer.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customer/bills")
@PreAuthorize("hasRole('CUSTOMER')")
@RequiredArgsConstructor
public class CustomerBillingController {

    private final BillingService billingService;
    private final CustomerRepository customerRepository;

    private Long getCustomerId(User user) {
        Customer customer = customerRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Customer profile not found"));
        return customer.getId();
    }

    @GetMapping
    public ResponseEntity<List<BillResponseDTO>> getMyBills(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(billingService.getCustomerBills(getCustomerId(user)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BillResponseDTO> getMyBillDetail(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return ResponseEntity.ok(billingService.getCustomerBillById(getCustomerId(user), id));
    }
}
