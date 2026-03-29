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
import org.springframework.scheduling.annotation.Scheduled;
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
        List<DeliveryRecord> records;
        if (deliveryPersonId != null) {
            records = deliveryRecordRepository.findByDeliveryDateAndDeliveryPersonId(date, deliveryPersonId);
        } else {
            records = deliveryRecordRepository.findByDeliveryDate(date);
        }

        List<Map<String, Object>> flatSchedule = new ArrayList<>();

        for (DeliveryRecord record : records) {
            Subscription sub = subscriptionRepository.findById(record.getSubscriptionId()).orElse(null);
            if (sub == null)
                continue;

            DeliveryPerson person = deliveryPersonRepository.findById(record.getDeliveryPersonId()).orElse(null);

            Map<String, Object> map = new HashMap<>();
            map.put("subscriptionId", sub.getId());
            map.put("customerId", sub.getCustomer().getId());
            map.put("customerName", sub.getCustomer().getName());
            map.put("address", sub.getCustomer().getAddress()
                    + (sub.getCustomer().getArea() != null ? " (" + sub.getCustomer().getArea() + ")" : ""));
            map.put("publicationName",
                    (sub.getPublications() == null || sub.getPublications().isEmpty()) ? ""
                            : sub.getPublications().stream().map(com.naas.backend.publication.Publication::getName)
                                    .collect(Collectors.joining(", ")));
            map.put("deliveryStatus", record.getStatus().name());
            map.put("assignedTo", person != null ? person.getName() : "Unknown");

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
            record.setPublicationId((sub.getPublications() == null || sub.getPublications().isEmpty()) ? 0L
                    : sub.getPublications().get(0).getId());
        }

        record.setStatus(status);
        deliveryRecordRepository.save(record);
    }

    /**
     * Nightly trigger to pre-generate delivery schedules into the database.
     * Currently commented out as requested.
     */
    // @Scheduled(cron = "0 1 0 * * ?") // Runs at 12:01 AM every day
    // public void scheduledDailyScheduleGeneration() {
    // System.out.println("Auto-generating daily delivery schedules for " +
    // LocalDate.now());
    // generateSchedulesForDate(LocalDate.now());
    // }

    /**
     * Generates and saves PENDING delivery records for a specific date for all
     * active subscriptions.
     * This makes the schedules physically exist in the database instead of creating
     * them on-the-fly.
     */
    public void generateSchedulesForDate(LocalDate date) {
        List<Subscription> allActive = subscriptionRepository.findAll().stream()
                .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE)
                .collect(Collectors.toList());

        List<DeliveryPerson> allDeliveryPersons = deliveryPersonRepository.findAll();
        if (allDeliveryPersons.isEmpty()) {
            return;
        }

        List<DeliveryPerson> globalPersons = allDeliveryPersons.stream()
                .filter(dp -> dp.getAssignedArea() == null || dp.getAssignedArea().trim().isEmpty()
                        || dp.getAssignedArea().equalsIgnoreCase("Global")
                        || dp.getAssignedArea().equalsIgnoreCase("All"))
                .collect(Collectors.toList());

        Map<String, Integer> roundRobinIndex = new HashMap<>();

        for (Subscription sub : allActive) {
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

            Optional<DeliveryRecord> existingRecord = deliveryRecordRepository.findByDeliveryDateAndSubscriptionId(date,
                    sub.getId());
            if (existingRecord.isPresent()) {
                continue;
            }

            String area = sub.getCustomer().getArea();
            List<DeliveryPerson> eligible = new ArrayList<>();

            if (area != null && !area.trim().isEmpty()) {
                String areaLower = area.toLowerCase();
                eligible = allDeliveryPersons.stream()
                        .filter(dp -> dp.getAssignedArea() != null
                                && dp.getAssignedArea().toLowerCase().contains(areaLower))
                        .collect(Collectors.toList());
            }

            if (eligible.isEmpty()) {
                eligible = globalPersons;
            }
            if (eligible.isEmpty()) {
                eligible = allDeliveryPersons;
            }

            String rrKey = (area != null && !area.trim().isEmpty()) ? area.toLowerCase() : "global";
            int idx = roundRobinIndex.getOrDefault(rrKey, 0);
            DeliveryPerson assignedPerson = eligible.get(idx % eligible.size());
            roundRobinIndex.put(rrKey, idx + 1);

            DeliveryRecord record = DeliveryRecord.builder()
                    .deliveryDate(date)
                    .deliveryPersonId(assignedPerson.getId())
                    .subscriptionId(sub.getId())
                    .customerId(sub.getCustomer().getId())
                    .publicationId((sub.getPublications() == null || sub.getPublications().isEmpty()) ? 0L
                            : sub.getPublications().get(0).getId())
                    .status(DeliveryRecord.DeliveryStatus.PENDING)
                    .build();
            deliveryRecordRepository.save(record);
        }
    }
}
