package com.naas.backend.subscription;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.naas.backend.publication.Publication;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "subscription_items")
public class SubscriptionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private Subscription subscription;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "publication_id", nullable = false)
    private Publication publication;

    @Column(nullable = false)
    @Builder.Default
    private String frequency = "DAILY";

    @Column(name = "custom_delivery_days")
    private String customDeliveryDays;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null)
            createdAt = LocalDateTime.now();
    }
}
