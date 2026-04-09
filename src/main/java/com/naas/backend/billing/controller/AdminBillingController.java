package com.naas.backend.billing.controller;

import java.util.UUID;

import com.naas.backend.billing.dto.BillResponseDTO;
import com.naas.backend.billing.dto.PaymentRequestDTO;
import com.naas.backend.billing.dto.PaymentResponseDTO;
import com.naas.backend.billing.service.BillingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/bills")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminBillingController {

    private final BillingService billingService;

    // ── Bill retrieval ────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<BillResponseDTO>> getAllBills() {
        return ResponseEntity.ok(billingService.getAllBills());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BillResponseDTO> getBillById(@PathVariable UUID id) {
        return ResponseEntity.ok(billingService.getBillById(id));
    }

    @GetMapping("/overdue")
    public ResponseEntity<List<BillResponseDTO>> getOverdueBills() {
        return ResponseEntity.ok(billingService.getOverdueBills());
    }

    // ── Bill generation ───────────────────────────────────────────────

    @PostMapping("/generate")
    public ResponseEntity<String> generateBillsManually(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {

        YearMonth targetMonth;
        if (year != null && month != null) {
            targetMonth = YearMonth.of(year, month);
        } else {
            targetMonth = YearMonth.now().minusMonths(1);
        }

        billingService.generateBillsForMonth(targetMonth);
        return ResponseEntity.ok("Bills generated successfully for " + targetMonth.toString());
    }

    // ── Bill status ───────────────────────────────────────────────────

    @PatchMapping("/{id}/status")
    public ResponseEntity<BillResponseDTO> markBillStatus(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        String status = body.getOrDefault("status", "PAID");
        return ResponseEntity.ok(billingService.markBillStatus(id, status));
    }

    // ── Payment recording (FR-PAY1–5) ────────────────────────────────

    @PostMapping("/{id}/payments")
    public ResponseEntity<PaymentResponseDTO> recordPayment(
            @PathVariable UUID id,
            @RequestBody PaymentRequestDTO request) {
        return ResponseEntity.ok(billingService.recordPayment(id, request));
    }

    @GetMapping("/{id}/payments")
    public ResponseEntity<List<PaymentResponseDTO>> getPaymentsForBill(@PathVariable UUID id) {
        return ResponseEntity.ok(billingService.getPaymentsForBill(id));
    }
}
