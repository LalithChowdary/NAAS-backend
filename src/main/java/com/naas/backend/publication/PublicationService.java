package com.naas.backend.publication;

import java.util.UUID;

import com.naas.backend.publication.dto.PublicationRequest;
import com.naas.backend.publication.dto.PublicationResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import com.naas.backend.publication.dto.CartCalculationRequest;
import com.naas.backend.publication.dto.CartCalculationResponse;
import com.naas.backend.publication.dto.CartItemRequest;
import com.naas.backend.publication.dto.CartItemResponse;
import java.util.ArrayList;

@Service
public class PublicationService {

    private final PublicationRepository publicationRepository;

    @Autowired
    public PublicationService(PublicationRepository publicationRepository) {
        this.publicationRepository = publicationRepository;
    }

    public List<PublicationResponse> getAllPublications() {
        return publicationRepository.findAllByOrderByIdDesc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<PublicationResponse> searchPublications(String name, Boolean enabled) {
        if (name != null && !name.isEmpty() && enabled != null) {
            return publicationRepository.findByNameContainingIgnoreCaseAndEnabledOrderByIdDesc(name, enabled).stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        } else if (name != null && !name.isEmpty()) {
            return publicationRepository.findByNameContainingIgnoreCaseOrderByIdDesc(name).stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        } else if (enabled != null) {
            return publicationRepository.findByEnabledOrderByIdDesc(enabled).stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        } else {
            return getAllPublications();
        }
    }

    public PublicationResponse getPublicationById(UUID id) {
        Publication publication = publicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Publication not found with id: " + id));
        return toResponse(publication);
    }

    public PublicationResponse createPublication(PublicationRequest request) {
        Publication publication = new Publication();
        publication.setName(request.getName());
        publication.setType(request.getType());
        publication.setPrice(request.getPrice());
        publication.setDescription(request.getDescription());
        publication.setFrequency(request.getFrequency());
        publication.setImageUrl(request.getImageUrl());

        Publication savedPublication = publicationRepository.save(publication);
        return toResponse(savedPublication);
    }

    public PublicationResponse updatePublication(UUID id, PublicationRequest request) {
        Publication publication = publicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Publication not found with id: " + id));

        if (request.getName() != null && !request.getName().isEmpty()) {
            publication.setName(request.getName());
        }
        if (request.getType() != null) {
            publication.setType(request.getType());
        }
        if (request.getPrice() != null) {
            publication.setPrice(request.getPrice());
        }
        if (request.getDescription() != null) {
            publication.setDescription(request.getDescription());
        }
        if (request.getFrequency() != null) {
            publication.setFrequency(request.getFrequency());
        }
        if (request.getImageUrl() != null) {
            publication.setImageUrl(request.getImageUrl());
        }

        Publication updatedPublication = publicationRepository.save(publication);
        return toResponse(updatedPublication);
    }

    public PublicationResponse toggleEnabled(UUID id, boolean enabled) {
        Publication publication = publicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Publication not found with id: " + id));

        publication.setEnabled(enabled);
        Publication updatedPublication = publicationRepository.save(publication);
        return toResponse(updatedPublication);
    }

    public CartCalculationResponse calculateMonthlyCost(CartCalculationRequest request) {
        CartCalculationResponse response = new CartCalculationResponse();
        double totalCost = 0.0;
        List<CartItemResponse> itemResponses = new ArrayList<>();

        if (request.getItems() != null) {
            for (CartItemRequest itemReq : request.getItems()) {
                Publication pub = publicationRepository.findById(itemReq.getPublicationId())
                        .orElseThrow(() -> new RuntimeException(
                                "Publication not found with id: " + itemReq.getPublicationId()));

                CartItemResponse itemResp = new CartItemResponse();
                itemResp.setPublicationId(pub.getId());
                itemResp.setName(pub.getName());
                itemResp.setFrequency(pub.getFrequency());
                itemResp.setPricePerIssue(pub.getPrice());
                itemResp.setQuantity(itemReq.getQuantity());

                double multiplier = 1.0;
                if (pub.getFrequency() != null) {
                    switch (pub.getFrequency().toUpperCase()) {
                        case "DAILY":
                            multiplier = 30.0;
                            break;
                        case "WEEKLY":
                            multiplier = 4.0;
                            break;
                        case "MONTHLY":
                            multiplier = 1.0;
                            break;
                        default:
                            multiplier = 1.0; // Fallback
                            break;
                    }
                }

                double itemMonthlyCost = pub.getPrice() * multiplier * itemReq.getQuantity();
                itemResp.setMonthlyCost(itemMonthlyCost);

                itemResponses.add(itemResp);
                totalCost += itemMonthlyCost;
            }
        }

        response.setItems(itemResponses);
        response.setTotalMonthlyCost(totalCost);
        return response;
    }

    private PublicationResponse toResponse(Publication publication) {
        PublicationResponse response = new PublicationResponse();
        response.setId(publication.getId());
        response.setName(publication.getName());
        response.setType(publication.getType());
        response.setPrice(publication.getPrice());
        response.setDescription(publication.getDescription());
        response.setFrequency(publication.getFrequency());
        response.setImageUrl(publication.getImageUrl());
        response.setEnabled(publication.isEnabled());
        response.setCreatedAt(publication.getCreatedAt());
        return response;
    }
}
