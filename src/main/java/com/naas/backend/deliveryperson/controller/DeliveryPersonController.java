package com.naas.backend.deliveryperson.controller;

import java.util.UUID;

import com.naas.backend.deliveryperson.DeliveryPerson;
import com.naas.backend.deliveryperson.service.DeliveryPersonService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

import com.naas.backend.deliveryperson.dto.PayoutRequest;
import com.naas.backend.deliveryperson.dto.PayoutResponse;

@RestController
@RequestMapping("/api/delivery-person")
@RequiredArgsConstructor
public class DeliveryPersonController {

    private final DeliveryPersonService deliveryPersonService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('DELIVERY_PERSON')")
    public ResponseEntity<DeliveryPerson> getMe() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return ResponseEntity.ok(deliveryPersonService.getByEmail(auth.getName()));
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('DELIVERY_PERSON')")
    public ResponseEntity<DeliveryPerson> updateMe(@RequestBody java.util.Map<String, String> body) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return ResponseEntity.ok(deliveryPersonService.updateProfile(
                auth.getName(),
                body.get("name"),
                body.get("phone"),
                body.get("payoutDetails")
        ));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DeliveryPerson>> getAll() {
        return ResponseEntity.ok(deliveryPersonService.getAllDeliveryPersonnel());
    }
    
    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DeliveryPerson>> getPendingRequests() {
        return ResponseEntity.ok(deliveryPersonService.getPendingRequests());
    }

    @PostMapping("/signup")
    public ResponseEntity<DeliveryPerson> signup(@RequestBody java.util.Map<String, String> body) {
        return ResponseEntity.ok(deliveryPersonService.signupRequest(
                body.get("name"),
                body.get("email"),
                body.get("password"),
                body.get("phone")));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DeliveryPerson> create(@RequestParam String name, @RequestParam String email,
            @RequestParam String password, @RequestParam String phone,
            @RequestParam String employeeId) {
        return ResponseEntity
                .ok(deliveryPersonService.createDeliveryPerson(name, email, password, phone, employeeId));
    }

    
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DeliveryPerson> approve(@PathVariable UUID id) {
        return ResponseEntity.ok(deliveryPersonService.approveDeliveryPerson(id));
    }
    
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DeliveryPerson> reject(@PathVariable UUID id) {
        return ResponseEntity.ok(deliveryPersonService.rejectDeliveryPerson(id));
    }

    @PutMapping("/{id}/toggleStatus")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DeliveryPerson> toggleStatus(@PathVariable UUID id, @RequestParam boolean active) {
        return ResponseEntity.ok(deliveryPersonService.toggleStatus(id, active));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DeliveryPerson> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(deliveryPersonService.getDeliveryPersonById(id));
    }

    @GetMapping("/{id}/deliveries")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<com.naas.backend.delivery.entity.DeliveryRecord>> getDeliveries(@PathVariable UUID id) {
        return ResponseEntity.ok(deliveryPersonService.getDeliveriesForDeliveryPerson(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DeliveryPerson> updateDeliveryPerson(@PathVariable UUID id, @RequestBody java.util.Map<String, String> body) {
        return ResponseEntity.ok(deliveryPersonService.updateDeliveryPersonAsAdmin(
                id,
                body.get("name"),
                body.get("phone"),
                body.get("employeeId"),
                body.get("payoutDetails")
        ));
    }

    @GetMapping("/{id}/payout")
    @PreAuthorize("hasAnyRole('ADMIN', 'DELIVERY_PERSON')")
    public ResponseEntity<Double> calculatePayout(@PathVariable UUID id, @RequestParam String start,
            @RequestParam String end) {
        return ResponseEntity
                .ok(deliveryPersonService.calculatePayout(id, LocalDate.parse(start), LocalDate.parse(end)));
    }

    @PostMapping("/{id}/payout")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PayoutResponse> processPayout(@PathVariable UUID id, @RequestBody PayoutRequest request) {
        return ResponseEntity.ok(deliveryPersonService.processPayout(id, request));
    }

    @GetMapping("/me/payouts")
    @PreAuthorize("hasRole('DELIVERY_PERSON')")
    public ResponseEntity<List<PayoutResponse>> getMyPayouts() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return ResponseEntity.ok(deliveryPersonService.getMyPayouts(auth.getName()));
    }
}
