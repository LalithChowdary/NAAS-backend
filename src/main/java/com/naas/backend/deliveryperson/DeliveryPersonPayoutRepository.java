package com.naas.backend.deliveryperson;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface DeliveryPersonPayoutRepository extends JpaRepository<DeliveryPersonPayout, UUID> {
    List<DeliveryPersonPayout> findByDeliveryPersonIdAndStartDateAndEndDate(UUID deliveryPersonId, LocalDate startDate, LocalDate endDate);
    List<DeliveryPersonPayout> findByDeliveryPersonId(UUID deliveryPersonId);
}
