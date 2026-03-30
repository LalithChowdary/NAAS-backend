package com.naas.backend.deliveryperson.controller;

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

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DeliveryPerson> create(@RequestParam String name, @RequestParam String email,
            @RequestParam String password, @RequestParam String phone,
            @RequestParam String employeeId, @RequestParam String assignedArea) {
        return ResponseEntity
                .ok(deliveryPersonService.createDeliveryPerson(name, email, password, phone, employeeId, assignedArea));
    }

    @PutMapping("/{id}/area")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DeliveryPerson> assignArea(@PathVariable Long id, @RequestParam String area) {
        return ResponseEntity.ok(deliveryPersonService.assignArea(id, area));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DeliveryPerson> updateDeliveryPerson(@PathVariable Long id, @RequestBody java.util.Map<String, String> body) {
        return ResponseEntity.ok(deliveryPersonService.updateDeliveryPersonAsAdmin(
                id,
                body.get("name"),
                body.get("phone"),
                body.get("employeeId"),
                body.get("assignedArea"),
                body.get("payoutDetails")
        ));
    }

    @GetMapping("/{id}/payout")
    @PreAuthorize("hasAnyRole('ADMIN', 'DELIVERY_PERSON')")
    public ResponseEntity<Double> calculatePayout(@PathVariable Long id, @RequestParam String start,
            @RequestParam String end) {
        return ResponseEntity
                .ok(deliveryPersonService.calculatePayout(id, LocalDate.parse(start), LocalDate.parse(end)));
    }
}
