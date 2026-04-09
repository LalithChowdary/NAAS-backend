package com.naas.backend.delivery.service;

import java.util.UUID;

import com.naas.backend.delivery.entity.DeliveryRecord;
import com.naas.backend.delivery.repository.DeliveryRecordRepository;
import com.naas.backend.deliveryperson.DeliveryPerson;
import com.naas.backend.deliveryperson.DeliveryPersonRepository;
import com.naas.backend.subscription.Subscription;
import com.naas.backend.subscription.SubscriptionRepository;
import com.naas.backend.subscription.SubscriptionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import com.naas.backend.delivery.dto.CustomerDeliveryResponse;
import com.naas.backend.subscription.SubscriptionItem;
import com.naas.backend.delivery.dto.DeliveryPersonHistoryResponse;
import com.naas.backend.auth.entity.User;

import com.naas.backend.customer.Customer;
import com.naas.backend.customer.CustomerRepository;

@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final SubscriptionRepository subscriptionRepository;
    private final DeliveryRecordRepository deliveryRecordRepository;
    private final DeliveryPersonRepository deliveryPersonRepository;
    private final CustomerRepository customerRepository;
    private final JdbcTemplate jdbcTemplate;

    public List<CustomerDeliveryResponse> getCustomerDeliveries(User user) {
        Customer customer = customerRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        List<DeliveryRecord> records = deliveryRecordRepository
                .findByCustomerIdOrderByDeliveryDateDesc(customer.getId());

        List<CustomerDeliveryResponse> responses = new ArrayList<>();

        for (DeliveryRecord record : records) {
            Subscription sub = subscriptionRepository.findById(record.getSubscriptionId()).orElse(null);
            if (sub == null)
                continue;

            String pubNames = (sub.getItems() == null || sub.getItems().isEmpty()) ? ""
                    : sub.getItems().stream().map(item -> item.getPublication().getName())
                            .collect(Collectors.joining(", "));

            double dailyCost = 0.0;
            if (sub.getItems() != null) {
                for (SubscriptionItem item : sub.getItems()) {
                    dailyCost += item.getPublication().getPrice();
                }
            }

            responses.add(CustomerDeliveryResponse.builder()
                    .id(record.getId())
                    .deliveryDate(record.getDeliveryDate())
                    .status(record.getStatus().name())
                    .publicationName(pubNames)
                    .dailyCost(dailyCost)
                    .build());
        }

        return responses;
    }

    public List<DeliveryPersonHistoryResponse> getDeliveryPersonHistory(User user) {
        DeliveryPerson person = deliveryPersonRepository.findByUser(user).orElseThrow();
        List<DeliveryRecord> records = deliveryRecordRepository.findAll().stream()
                .filter(r -> r.getDeliveryPersonId().equals(person.getId()))
                .sorted((a, b) -> b.getDeliveryDate().compareTo(a.getDeliveryDate()))
                .collect(java.util.stream.Collectors.toList());

        return records.stream().map(record -> {
            Subscription sub = subscriptionRepository.findById(record.getSubscriptionId()).orElse(null);
            if (sub == null)
                return null;

            double total = sub.getItems().stream().mapToDouble(i -> i.getPublication().getPrice().doubleValue()).sum();
            String pubs = sub.getItems().stream().map(i -> i.getPublication().getName())
                    .collect(java.util.stream.Collectors.joining(", "));

            return DeliveryPersonHistoryResponse.builder()
                    .id(record.getId())
                    .subscriptionId(sub.getId())
                    .deliveryDate(record.getDeliveryDate())
                    .status(record.getStatus().name())
                    .customerName(sub.getCustomer().getName())
                    .customerAddress(sub.getCustomerAddress() != null ? sub.getCustomerAddress().getAddress() : "No Address")
                    .publications(pubs)
                    .totalValue(total)
                    .payout(total * 0.025)
                    .build();
        }).filter(r -> r != null).collect(java.util.stream.Collectors.toList());
    }

    public List<Map<String, Object>> getDailyDeliverySchedule(UUID deliveryPersonId, LocalDate date) {
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
            map.put("address", sub.getCustomerAddress() != null ? sub.getCustomerAddress().getAddress() : "No Address");
            map.put("publicationName",
                    (sub.getItems() == null || sub.getItems().isEmpty()) ? ""
                            : sub.getItems().stream().map(item -> item.getPublication().getName())
                                    .collect(Collectors.joining(", ")));
            map.put("deliveryStatus", record.getStatus().name());
            map.put("assignedTo", person != null ? person.getName() : "Unknown");

            flatSchedule.add(map);
        }

        return flatSchedule;
    }

    public void updateDeliveryStatus(LocalDate date, UUID deliveryPersonId, UUID subscriptionId,
            DeliveryRecord.DeliveryStatus status) {
        List<DeliveryRecord> records = deliveryRecordRepository.findByDeliveryDateAndSubscriptionId(date,
                subscriptionId);
        DeliveryRecord record = records.isEmpty() ? DeliveryRecord.builder()
                .deliveryDate(date)
                .deliveryPersonId(deliveryPersonId)
                .subscriptionId(subscriptionId)
                .build() : records.get(0);

        if (record.getId() == null) {
            // syncDeliveryRecordIdSequence();
            Subscription sub = subscriptionRepository.findById(subscriptionId).orElseThrow();
            record.setCustomerId(sub.getCustomer().getId());
            record.setPublicationId((sub.getItems() == null || sub.getItems().isEmpty()) ? null
                    : sub.getItems().get(0).getPublication().getId());
        }

        record.setStatus(status);
        saveDeliveryRecordWithRetry(record);
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
        // syncDeliveryRecordIdSequence();

        List<Subscription> allActive = subscriptionRepository.findAll().stream()
                .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE)
                .collect(Collectors.toList());

        List<DeliveryPerson> allDeliveryPersons = deliveryPersonRepository.findAll();
        if (allDeliveryPersons.isEmpty()) {
            return;
        }

        List<DeliveryPerson> globalPersons = allDeliveryPersons;

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

            boolean existingRecord = deliveryRecordRepository.existsByDeliveryDateAndSubscriptionId(date,
                    sub.getId());
            if (existingRecord) {
                continue;
            }

            String area = ""; // Deprecated field
            List<DeliveryPerson> eligible = new ArrayList<>();

            

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
                    .publicationId((sub.getItems() == null || sub.getItems().isEmpty()) ? null
                            : sub.getItems().get(0).getPublication().getId())
                    .status(DeliveryRecord.DeliveryStatus.PENDING)
                    .build();
            saveDeliveryRecordWithRetry(record);
        }
    }

    private void syncDeliveryRecordIdSequence() { }

    private void saveDeliveryRecordWithRetry(DeliveryRecord record) {
        try {
            deliveryRecordRepository.save(record);
        } catch (DataIntegrityViolationException ex) {
            if (!isDeliveryRecordPrimaryKeyConflict(ex)) {
                throw ex;
            }

            // syncDeliveryRecordIdSequence();
            deliveryRecordRepository.save(record);
        }
    }

    private boolean isDeliveryRecordPrimaryKeyConflict(DataIntegrityViolationException ex) {
        String message = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();
        return message != null && message.contains("delivery_records_pkey");
    }
}
