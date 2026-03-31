package com.naas.backend.billing.repository;

import com.naas.backend.billing.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByBillIdOrderByPaidAtDesc(Long billId);
    List<Payment> findByCustomerIdOrderByPaidAtDesc(Long customerId);
}
