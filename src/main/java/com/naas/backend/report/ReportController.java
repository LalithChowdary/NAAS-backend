package com.naas.backend.report;

import com.naas.backend.report.dto.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@PreAuthorize("hasRole('ADMIN')")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/monthly-summary")
    public ResponseEntity<MonthlySummaryResponse> getMonthlySummary(
            @RequestParam String month) {
        return ResponseEntity.ok(reportService.getMonthlySummary(month));
    }

    @GetMapping("/outstanding-dues")
    public ResponseEntity<List<OutstandingDuesResponse>> getOutstandingDues() {
        return ResponseEntity.ok(reportService.getOutstandingDues());
    }

    @GetMapping("/delivery-summary")
    public ResponseEntity<List<DeliverySummaryResponse>> getDeliverySummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(reportService.getDeliverySummary(startDate, endDate));
    }

    @GetMapping("/delivery-personnel-payment")
    public ResponseEntity<List<DeliveryPersonnelPaymentResponse>> getDeliveryPersonnelPayment(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(reportService.getDeliveryPersonnelPayment(startDate, endDate));
    }

    @GetMapping("/who-received-what")
    public ResponseEntity<List<WhoReceivedWhatResponse>> getWhoReceivedWhat(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(reportService.getWhoReceivedWhat(startDate, endDate));
    }
}
