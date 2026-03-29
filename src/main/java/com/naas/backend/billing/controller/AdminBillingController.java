package com.naas.backend.billing.controller;

import com.naas.backend.billing.dto.BillResponseDTO;
import com.naas.backend.billing.service.BillingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/admin/bills")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminBillingController {

    private final BillingService billingService;

    @GetMapping
    public ResponseEntity<List<BillResponseDTO>> getAllBills() {
        return ResponseEntity.ok(billingService.getAllBills());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BillResponseDTO> getBillById(@PathVariable Long id) {
        return ResponseEntity.ok(billingService.getBillById(id));
    }

    @PostMapping("/generate")
    public ResponseEntity<String> generateBillsManually(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {

        YearMonth targetMonth;
        if (year != null && month != null) {
            targetMonth = YearMonth.of(year, month);
        } else {
            // Default to previous month
            targetMonth = YearMonth.now().minusMonths(1);
        }

        billingService.generateBillsForMonth(targetMonth);
        return ResponseEntity.ok("Bills generated successfully for " + targetMonth.toString());
    }
}
