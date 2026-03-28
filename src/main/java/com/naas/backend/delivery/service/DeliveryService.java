package com.naas.backend.delivery.service;

import com.naas.backend.delivery.dto.DeliveryScheduleResponse;
import com.naas.backend.delivery.entity.DeliveryRecord;
import com.naas.backend.delivery.repository.DeliveryRecordRepository;
import com.naas.backend.deliveryperson.DeliveryPerson;
import com.naas.backend.deliveryperson.DeliveryPersonRepository;
import com.naas.backend.subscription.Subscription;
import com.naas.backend.subscription.SubscriptionRepository;
import com.naas.backend.subscription.SubscriptionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final SubscriptionRepository subscriptionRepository;
    private final DeliveryRecordRepository deliveryRecordRepository;
    private final DeliveryPersonRepository deliveryPersonRepository;

    public List<Map<String, Object>> getDailyDeliverySchedule(Long deliveryPersonId, LocalDate date) {
        DeliveryPerson person = deliveryPersonRepository.findById(deliveryPersonId)
                .orElseThrow(() -> new RuntimeException("Delivery person not found"));

        String area = person.getAssignedArea();

        List<Subscription> activeSubscriptions;
        if (area == null || area.trim().isEmpty() || area.equalsIgnoreCase("Global") || area.equalsIgnoreCase("All")) {
            activeSubscriptions = subscriptionRepository.findAll().stream()
                    .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE)
                    .collect(Collectors.toList());
        } else {
            activeSubscriptions = subscriptionRepository.findByCustomer_AreaAndStatus(area, SubscriptionStatus.ACTIVE);

            if (activeSubscriptions.isEmpty()) {
                activeSubscriptions = subscriptionRepository.findAll().stream()
                        .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE)
                        .filter(s -> s.getCustomer().getArea() != null
                                && s.getCustomer().getArea().toLowerCase().contains(area.toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        List<DeliveryRecord> existingRecords = deliveryRecordRepository.findByDeliveryDateAndDeliveryPersonId(date,
                deliveryPersonId);
        Map<Long, DeliveryRecord.DeliveryStatus> overrideStatuses = existingRecords.stream()
                .collect(Collectors.toMap(DeliveryRecord::getSubscriptionId, DeliveryRecord::getStatus));

        List<Map<String, Object>> flatSchedule = new ArrayList<>();

        for (Subscription sub : activeSubscriptions) {
            if (sub.getStartDate() != null && sub.getStartDate().isAfter(date)) {
                continue;
            }
            if (sub.getEndDate() != null && sub.getEndDate().isBefore(date)) {
                continue;
            }
            if (sub.getSuspendStartDate() != null && sub.getSuspendEndDate() != null) {
                if ((date.isEqual(sub.getSuspendStartDate()) || date.isAfter(sub.getSuspendStartDate())) &&
                        (date.isEqual(sub.getSuspendEndDate()) || date.isBefore(sub.getSuspendEndDate()))) {
                    continue;
                }
            }

            Map<String, Object> map = new HashMap<>();
            map.put("subscriptionId", sub.getId());
            map.put("customerId", sub.getCustomer().getId());
            map.put("customerName", sub.getCustomer().getName());
            map.put("address", sub.getCustomer().getAddress()
                    + (sub.getCustomer().getArea() != null ? " (" + sub.getCustomer().getArea() + ")" : ""));
            map.put("publicationName", sub.getPublication().getName());
            map.put("deliveryStatus",
                    overrideStatuses.getOrDefault(sub.getId(), DeliveryRecord.DeliveryStatus.PENDING).name());

            flatSchedule.add(map);
        }

        return flatSchedule;
    }

    public void updateDeliveryStatus(LocalDate date, Long deliveryPersonId, Long subscriptionId,
            DeliveryRecord.DeliveryStatus status) {
        DeliveryRecord record = deliveryRecordRepository.findByDeliveryDateAndSubscriptionId(date, subscriptionId)
                .orElse(DeliveryRecord.builder()
                        .deliveryDate(date)
                        .deliveryPersonId(deliveryPersonId)
                        .subscriptionId(subscriptionId)
                        // Will fetch customer/publication if needed or default 0s if they are
                        // decoupled,
                        // but better to fetch sub to fill correctly.
                        .build());

        if (record.getId() == null) {
            Subscription sub = subscriptionRepository.findById(subscriptionId).orElseThrow();
            record.setCustomerId(sub.getCustomer().getId());
            record.setPublicationId(sub.getPublication().getId());
        }

        record.setStatus(status);
        deliveryRecordRepository.save(record);
    }
}
