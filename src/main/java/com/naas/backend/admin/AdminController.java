package com.naas.backend.admin;

import com.naas.backend.admin.dto.CreateAdminRequest;
import com.naas.backend.admin.dto.CreateDeliveryPersonRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @PostMapping("/create-admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> createAdmin(@Valid @RequestBody CreateAdminRequest request) {
        return ResponseEntity.ok(adminService.createAdmin(request));
    }

    @PostMapping("/create-delivery-person")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> createDeliveryPerson(@Valid @RequestBody CreateDeliveryPersonRequest request) {
        return ResponseEntity.ok(adminService.createDeliveryPerson(request));
    }
}
