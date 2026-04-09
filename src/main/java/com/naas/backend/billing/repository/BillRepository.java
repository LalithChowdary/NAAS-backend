package com.naas.backend.billing.repository;

import java.util.UUID;

import com.naas.backend.billing.entity.Bill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BillRepository extends JpaRepository<Bill, UUID> {
    List<Bill> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);

    Optional<Bill> findByCustomerIdAndBillingMonth(UUID customerId, String billingMonth);

    List<Bill> findAllByOrderByCreatedAtDesc();
}
