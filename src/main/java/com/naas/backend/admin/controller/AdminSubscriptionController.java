package com.naas.backend.admin.controller;

import java.util.UUID;

import com.naas.backend.subscription.SubscriptionService;
import com.naas.backend.subscription.dto.GlobalDeliveryPauseResponse;
import com.naas.backend.subscription.dto.SubscriptionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api/admin/subscriptions")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminSubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping
    public ResponseEntity<List<SubscriptionResponse>> getAllSubscriptions() {
        return ResponseEntity.ok(subscriptionService.getAllSubscriptions());
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<SubscriptionResponse>> getCustomerSubscriptions(@PathVariable UUID customerId) {
        return ResponseEntity.ok(subscriptionService.getCustomerSubscriptions(customerId));
    }

    @GetMapping("/customer/{customerId}/pauses")
    public ResponseEntity<List<GlobalDeliveryPauseResponse>> getCustomerGlobalPauses(@PathVariable UUID customerId) {
        return ResponseEntity.ok(subscriptionService.getCustomerGlobalPauses(customerId));
    }
}
