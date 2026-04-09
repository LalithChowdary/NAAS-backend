package com.naas.backend.billing.repository;

import java.util.UUID;

import com.naas.backend.billing.entity.BillItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillItemRepository extends JpaRepository<BillItem, UUID> {
}
