package com.naas.backend.billing.controller;

import com.naas.backend.auth.entity.User;
import com.naas.backend.billing.dto.BillResponseDTO;
import com.naas.backend.billing.dto.PaymentResponseDTO;
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
@RequestMapping("/api/customer")
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

    @GetMapping("/bills")
    public ResponseEntity<List<BillResponseDTO>> getMyBills(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(billingService.getCustomerBills(getCustomerId(user)));
    }

    @GetMapping("/bills/{id}")
    public ResponseEntity<BillResponseDTO> getMyBillDetail(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return ResponseEntity.ok(billingService.getCustomerBillById(getCustomerId(user), id));
    }

    @GetMapping("/payments")
    public ResponseEntity<List<PaymentResponseDTO>> getMyPayments(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(billingService.getCustomerPayments(getCustomerId(user)));
    }
}
