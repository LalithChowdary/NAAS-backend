package com.naas.backend.subscription;

import com.naas.backend.customer.Customer;
import com.naas.backend.publication.Publication;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "subscriptions")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Builder.Default
    @OneToMany(mappedBy = "subscription", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private java.util.List<SubscriptionItem> items = new java.util.ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SubscriptionStatus status = SubscriptionStatus.ACTIVE;

    @Column(nullable = false)
    private LocalDate startDate;

    private LocalDate endDate;

    private LocalDate suspendStartDate;

    private LocalDate suspendEndDate;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
