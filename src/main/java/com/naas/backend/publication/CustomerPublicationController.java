package com.naas.backend.publication;

import com.naas.backend.publication.dto.PublicationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/publications")
@RequiredArgsConstructor
public class CustomerPublicationController {

    private final PublicationService publicationService;

    // Customers should primarily see enabled ones. Alternatively, the frontend drops the ones that are not enabled.
    // If the backend has strictly role secured logic, we can expose this to everyone (or secured without Role='ADMIN')
    @GetMapping
    public ResponseEntity<List<PublicationResponse>> getActivePublications() {
        return ResponseEntity.ok(publicationService.searchPublications(null, true));
    }
}
