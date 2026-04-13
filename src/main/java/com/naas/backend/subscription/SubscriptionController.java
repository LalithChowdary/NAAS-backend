package com.naas.backend.subscription;

import java.util.UUID;

import com.naas.backend.auth.entity.User;
import com.naas.backend.customer.Customer;
import com.naas.backend.customer.CustomerRepository;
import com.naas.backend.subscription.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

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

    private UUID getCustomerId(User user) {
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
            @PathVariable UUID id,
            @RequestBody CancelSubscriptionRequest request) {
        return ResponseEntity.ok(subscriptionService.cancelSubscription(getCustomerId(user), id, request));
    }

    @PutMapping("/{id}/suspend")
    public ResponseEntity<SubscriptionResponse> suspendSubscription(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id,
            @RequestBody SuspendSubscriptionRequest request) {
        return ResponseEntity.ok(subscriptionService.suspendSubscription(getCustomerId(user), id, request));
    }

    @DeleteMapping("/{id}/suspend")
    public ResponseEntity<SubscriptionResponse> removeSubscriptionSuspension(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {
        return ResponseEntity.ok(subscriptionService.removeSubscriptionSuspension(getCustomerId(user), id));
    }

    @DeleteMapping("/{subscriptionId}/items/{itemId}/suspend")
    public ResponseEntity<SubscriptionResponse> removeSubscriptionItemSuspension(
            @AuthenticationPrincipal User user,
            @PathVariable UUID subscriptionId,
            @PathVariable UUID itemId) {
        return ResponseEntity
                .ok(subscriptionService.removeSubscriptionItemSuspension(getCustomerId(user), subscriptionId, itemId));
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

    @PatchMapping("/{subscriptionId}/items/{itemId}")
    public ResponseEntity<SubscriptionResponse> updateItemStatus(
            @AuthenticationPrincipal User user,
            @PathVariable UUID subscriptionId,
            @PathVariable UUID itemId,
            @RequestBody ItemStatusRequest request) {
        return ResponseEntity
                .ok(subscriptionService.updateItemStatus(getCustomerId(user), subscriptionId, itemId, request));
    }
}
