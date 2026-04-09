package com.naas.backend.billing.repository;

import java.util.UUID;

import com.naas.backend.billing.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    List<Payment> findByBillIdOrderByPaidAtDesc(UUID billId);
    List<Payment> findByCustomerIdOrderByPaidAtDesc(UUID customerId);
}
