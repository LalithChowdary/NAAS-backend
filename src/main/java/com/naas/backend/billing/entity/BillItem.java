package com.naas.backend.billing.entity;

import com.naas.backend.publication.Publication;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "bill_items")
public class BillItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_id", nullable = false)
    private Bill bill;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publication_id", nullable = false)
    private Publication publication;

    // Days delivered for this publication in this billing month
    @Column(nullable = false)
    private int deliveriesCount;

    // Locked price of the publication at the time of bill generation
    @Column(nullable = false)
    private BigDecimal pricePerUnit;

    @Column(nullable = false)
    private BigDecimal itemAmount;
}
