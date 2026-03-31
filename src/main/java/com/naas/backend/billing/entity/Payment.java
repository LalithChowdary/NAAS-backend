package com.naas.backend.billing.entity;

import com.naas.backend.customer.Customer;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "payments")
public class Payment {

    public enum PaymentMethod {
        CASH, CHEQUE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_id", nullable = false)
    private Bill bill;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    // Only populated for CHEQUE payments
    private String chequeNumber;

    private String receiptNote;

    @Builder.Default
    @Column(name = "paid_at", nullable = false)
    private LocalDateTime paidAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (paidAt == null) paidAt = LocalDateTime.now();
    }
}
