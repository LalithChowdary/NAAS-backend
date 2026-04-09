package com.naas.backend.delivery.entity;

import java.util.UUID;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "delivery_records")
public class DeliveryRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private LocalDate deliveryDate;

    @Column(nullable = false)
    private UUID deliveryPersonId;

    @Column(nullable = false)
    private UUID customerId;

    @Column(name = "publication_id", nullable = true)
    private UUID publicationId;

    @Column(nullable = false)
    private UUID subscriptionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryStatus status;

    public enum DeliveryStatus {
        PENDING,
        DELIVERED,
        CANCELLED
    }
}
