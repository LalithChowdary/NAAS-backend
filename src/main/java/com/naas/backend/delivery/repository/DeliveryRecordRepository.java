package com.naas.backend.delivery.repository;

import com.naas.backend.delivery.entity.DeliveryRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryRecordRepository extends JpaRepository<DeliveryRecord, Long> {

        List<DeliveryRecord> findByDeliveryDateAndDeliveryPersonId(LocalDate deliveryDate, Long deliveryPersonId);

        List<DeliveryRecord> findByDeliveryDate(LocalDate deliveryDate);

        Optional<DeliveryRecord> findByDeliveryDateAndSubscriptionId(LocalDate deliveryDate, Long subscriptionId);

        List<DeliveryRecord> findByDeliveryPersonIdAndDeliveryDateBetweenAndStatus(
                        Long deliveryPersonId, LocalDate startDate, LocalDate endDate,
                        DeliveryRecord.DeliveryStatus status);

        long countByCustomerIdAndSubscriptionIdAndDeliveryDateBetweenAndStatus(
                        Long customerId, Long subscriptionId, LocalDate startDate, LocalDate endDate,
                        DeliveryRecord.DeliveryStatus status);
}
