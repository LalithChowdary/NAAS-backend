package com.naas.backend.subscription;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GlobalDeliveryPauseRepository extends JpaRepository<GlobalDeliveryPause, UUID> {
    List<GlobalDeliveryPause> findByCustomerId(UUID customerId);
}
