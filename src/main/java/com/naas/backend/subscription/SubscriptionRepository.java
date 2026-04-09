package com.naas.backend.subscription;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    List<Subscription> findByCustomerId(UUID customerId);

    List<Subscription> findByCustomerIdAndStatus(UUID customerId, SubscriptionStatus status);

    Optional<Subscription> findByIdAndCustomerId(UUID id, UUID customerId);
}
