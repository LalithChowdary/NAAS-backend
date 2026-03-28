package com.naas.backend.publication;

import com.naas.backend.publication.dto.PublicationRequest;
import com.naas.backend.publication.dto.PublicationResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/publications")
@PreAuthorize("hasRole('ADMIN')")
public class PublicationController {

    private final PublicationService publicationService;

    @Autowired
    public PublicationController(PublicationService publicationService) {
        this.publicationService = publicationService;
    }

    @PostMapping
    public ResponseEntity<PublicationResponse> createPublication(@RequestBody PublicationRequest request) {
        PublicationResponse response = publicationService.createPublication(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PublicationResponse> getPublicationDetails(@PathVariable Long id) {
        PublicationResponse response = publicationService.getPublicationById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PublicationResponse> updatePublication(
            @PathVariable Long id, 
            @RequestBody PublicationRequest request) {
        PublicationResponse response = publicationService.updatePublication(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<PublicationResponse> enableDisablePublication(
            @PathVariable Long id, 
            @RequestBody Map<String, Boolean> statusUpdate) {
        boolean enabled = statusUpdate.getOrDefault("enabled", false);
        PublicationResponse response = publicationService.toggleEnabled(id, enabled);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<PublicationResponse>> searchPublications(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean enabled) {
        List<PublicationResponse> responses = publicationService.searchPublications(search, enabled);
        return ResponseEntity.ok(responses);
    }
}
