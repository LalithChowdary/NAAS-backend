package com.naas.backend.hub;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/hubs")
@RequiredArgsConstructor
public class HubController {

    private final HubService hubService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Hub>> getAllHubs() {
        return ResponseEntity.ok(hubService.getAllHubs());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Hub> createHub(@RequestBody Hub hub) {
        return ResponseEntity.ok(hubService.createHub(hub));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Hub> updateHub(@PathVariable UUID id, @RequestBody Hub hub) {
        return ResponseEntity.ok(hubService.updateHub(id, hub));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteHub(@PathVariable UUID id) {
        hubService.deleteHub(id);
        return ResponseEntity.noContent().build();
    }
}
