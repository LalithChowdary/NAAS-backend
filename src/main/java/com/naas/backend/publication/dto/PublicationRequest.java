package com.naas.backend.publication.dto;

import com.naas.backend.publication.PublicationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class PublicationRequest {
    // Shared between create and update
    // For update, these might not be required technically depending on logic, but keeping it simple as per instructions.

    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Type is required")
    private PublicationType type;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    private Double price;

    private String description;

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public PublicationType getType() { return type; }
    public void setType(PublicationType type) { this.type = type; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
