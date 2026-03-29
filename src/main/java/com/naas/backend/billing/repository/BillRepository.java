package com.naas.backend.billing.repository;

import com.naas.backend.billing.entity.Bill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BillRepository extends JpaRepository<Bill, Long> {
    List<Bill> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    Optional<Bill> findByCustomerIdAndBillingMonth(Long customerId, String billingMonth);

    List<Bill> findAllByOrderByCreatedAtDesc();
}
