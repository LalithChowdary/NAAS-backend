package com.naas.backend.delivery.controller;

import com.naas.backend.auth.entity.User;
import com.naas.backend.delivery.dto.CustomerDeliveryResponse;
import com.naas.backend.delivery.dto.DeliveryPersonHistoryResponse;
import com.naas.backend.delivery.entity.DeliveryRecord;
import com.naas.backend.delivery.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/delivery")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;

    @GetMapping("/customer")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<CustomerDeliveryResponse>> getCustomerDeliveries(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(deliveryService.getCustomerDeliveries(user));
    }

    @GetMapping("/schedule")
    @PreAuthorize("hasAnyRole('ADMIN', 'DELIVERY_PERSON')")
    public ResponseEntity<List<Map<String, Object>>> getDailySchedule(
            @RequestParam(required = false) Long deliveryPersonId,
            @RequestParam(required = false) String date) {

        LocalDate queryDate = date != null ? LocalDate.parse(date) : LocalDate.now();
        return ResponseEntity.ok(deliveryService.getDailyDeliverySchedule(deliveryPersonId, queryDate));
    }

    @GetMapping("/person/history")
    @PreAuthorize("hasRole('DELIVERY_PERSON')")
    public ResponseEntity<List<DeliveryPersonHistoryResponse>> getDeliveryPersonHistory(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(deliveryService.getDeliveryPersonHistory(user));
    }

    @PostMapping("/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'DELIVERY_PERSON')")
    public ResponseEntity<Void> updateDeliveryStatus(
            @RequestParam Long deliveryPersonId,
            @RequestParam Long subscriptionId,
            @RequestParam String date,
            @RequestParam DeliveryRecord.DeliveryStatus status) {

        deliveryService.updateDeliveryStatus(LocalDate.parse(date), deliveryPersonId, subscriptionId, status);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/admin/generate-schedule")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> generateScheduleForDate(@RequestParam(required = false) String date) {
        LocalDate targetDate = date != null ? LocalDate.parse(date) : LocalDate.now();
        deliveryService.generateSchedulesForDate(targetDate);
        return ResponseEntity.ok("Successfully generated delivery schedules for " + targetDate);
    }
}
