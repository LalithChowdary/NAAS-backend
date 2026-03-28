package com.naas.backend.subscription;

import com.naas.backend.auth.entity.User;
import com.naas.backend.customer.Customer;
import com.naas.backend.customer.CustomerRepository;
import com.naas.backend.subscription.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customer/subscriptions")
@PreAuthorize("hasRole('CUSTOMER')")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final CustomerRepository customerRepository;

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body("{\"message\": \"" + e.getMessage() + "\"}");
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRuntimeException(RuntimeException e) {
        return ResponseEntity.badRequest().body("{\"message\": \"" + e.getMessage() + "\"}");
    }

    private Long getCustomerId(User user) {
        Customer customer = customerRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Customer profile not found"));
        return customer.getId();
    }

    @GetMapping
    public ResponseEntity<List<SubscriptionResponse>> getMySubscriptions(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(subscriptionService.getCustomerSubscriptions(getCustomerId(user)));
    }

    @PostMapping
    public ResponseEntity<List<SubscriptionResponse>> createSubscription(
            @AuthenticationPrincipal User user,
            @RequestBody CreateSubscriptionRequest request) {
        return ResponseEntity.ok(subscriptionService.createSubscription(getCustomerId(user), request));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<SubscriptionResponse> cancelSubscription(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestBody CancelSubscriptionRequest request) {
        return ResponseEntity.ok(subscriptionService.cancelSubscription(getCustomerId(user), id, request));
    }

    @PutMapping("/{id}/suspend")
    public ResponseEntity<SubscriptionResponse> suspendSubscription(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestBody SuspendSubscriptionRequest request) {
        return ResponseEntity.ok(subscriptionService.suspendSubscription(getCustomerId(user), id, request));
    }

    @GetMapping("/pause")
    public ResponseEntity<List<GlobalDeliveryPauseResponse>> getGlobalPauses(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(subscriptionService.getCustomerGlobalPauses(getCustomerId(user)));
    }

    @PostMapping("/pause")
    public ResponseEntity<GlobalDeliveryPauseResponse> addGlobalPause(
            @AuthenticationPrincipal User user,
            @RequestBody GlobalDeliveryPauseRequest request) {
        return ResponseEntity.ok(subscriptionService.addGlobalPause(getCustomerId(user), request));
    }
}
