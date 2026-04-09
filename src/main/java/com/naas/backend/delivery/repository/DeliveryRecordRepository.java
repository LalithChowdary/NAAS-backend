package com.naas.backend.delivery.repository;

import java.util.UUID;

import com.naas.backend.delivery.entity.DeliveryRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DeliveryRecordRepository extends JpaRepository<DeliveryRecord, UUID> {

        List<DeliveryRecord> findByDeliveryDateAndDeliveryPersonId(LocalDate deliveryDate, UUID deliveryPersonId);

        List<DeliveryRecord> findByDeliveryDate(LocalDate deliveryDate);

        List<DeliveryRecord> findByDeliveryDateAndSubscriptionId(LocalDate deliveryDate, UUID subscriptionId);

        boolean existsByDeliveryDateAndSubscriptionId(LocalDate deliveryDate, UUID subscriptionId);

        List<DeliveryRecord> findByDeliveryPersonIdAndDeliveryDateBetweenAndStatus(
                        UUID deliveryPersonId, LocalDate startDate, LocalDate endDate,
                        DeliveryRecord.DeliveryStatus status);

        long countByCustomerIdAndSubscriptionIdAndDeliveryDateBetweenAndStatus(
                        UUID customerId, UUID subscriptionId, LocalDate startDate, LocalDate endDate,
                        DeliveryRecord.DeliveryStatus status);

        List<DeliveryRecord> findByCustomerIdOrderByDeliveryDateDesc(UUID customerId);
}
