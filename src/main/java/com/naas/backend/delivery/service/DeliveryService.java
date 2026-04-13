package com.naas.backend.delivery.service;

import java.util.UUID;

import com.naas.backend.delivery.entity.DeliveryRecord;
import com.naas.backend.delivery.repository.DeliveryRecordRepository;
import com.naas.backend.deliveryperson.DeliveryPerson;
import com.naas.backend.deliveryperson.DeliveryPersonRepository;
import com.naas.backend.hub.Hub;
import com.naas.backend.hub.HubRepository;
import com.naas.backend.subscription.Subscription;
import com.naas.backend.subscription.SubscriptionRepository;
import com.naas.backend.subscription.SubscriptionStatus;
import com.naas.backend.subscription.SubscriptionStatus;
import com.naas.backend.subscription.SubscriptionItemStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final SubscriptionRepository subscriptionRepository;
    private final DeliveryRecordRepository deliveryRecordRepository;
    private final DeliveryPersonRepository deliveryPersonRepository;
    private final CustomerRepository customerRepository;
    private final HubRepository hubRepository;
    private final JdbcTemplate jdbcTemplate;
    private final FleetRoutingService fleetRoutingService;

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
                    : sub.getItems().stream()
                            .filter(item -> isItemActive(item, record.getDeliveryDate()))
                            .map(item -> item.getPublication().getName())
                            .collect(Collectors.joining(", "));

            double dailyCost = 0.0;
            if (sub.getItems() != null) {
                for (SubscriptionItem item : sub.getItems()) {
                    if (isItemActive(item, record.getDeliveryDate())) {
                        dailyCost += item.getPublication().getPrice();
                    }
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

            double total = sub.getItems().stream().filter(i -> isItemActive(i, record.getDeliveryDate()))
                    .mapToDouble(i -> i.getPublication().getPrice().doubleValue()).sum();
            String pubs = sub.getItems().stream().filter(i -> isItemActive(i, record.getDeliveryDate()))
                    .map(i -> i.getPublication().getName())
                    .collect(java.util.stream.Collectors.joining(", "));

            return DeliveryPersonHistoryResponse.builder()
                    .id(record.getId())
                    .subscriptionId(sub.getId())
                    .deliveryDate(record.getDeliveryDate())
                    .status(record.getStatus().name())
                    .customerName(sub.getCustomer().getName())
                    .customerAddress(
                            sub.getCustomerAddress() != null ? sub.getCustomerAddress().getAddress() : "No Address")
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

            Hub hub = record.getHubId() != null ? hubRepository.findById(record.getHubId()).orElse(null) : null;

            map.put("subscriptionId", sub.getId());
            map.put("customerId", sub.getCustomer().getId());
            map.put("customerName", sub.getCustomer().getName());
            map.put("address", sub.getCustomerAddress() != null ? sub.getCustomerAddress().getAddress() : "No Address");
            map.put("latitude", sub.getCustomerAddress() != null ? sub.getCustomerAddress().getLatitude() : null);
            map.put("longitude", sub.getCustomerAddress() != null ? sub.getCustomerAddress().getLongitude() : null);
            map.put("publicationName",
                    (sub.getItems() == null || sub.getItems().isEmpty()) ? ""
                            : sub.getItems().stream().filter(item -> isItemActive(item, record.getDeliveryDate()))
                                    .map(item -> item.getPublication().getName())
                                    .collect(Collectors.joining(", ")));
            map.put("deliveryStatus", record.getStatus().name());
            map.put("assignedTo", person != null ? person.getName() : "Unknown");
            map.put("deliveryPersonId", person != null ? person.getId() : null);
            map.put("routeSequence", record.getRouteSequence());
            map.put("hubName", hub != null ? hub.getName() : null);
            map.put("hubLat", hub != null ? hub.getLatitude() : null);
            map.put("hubLng", hub != null ? hub.getLongitude() : null);

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
            Subscription sub = subscriptionRepository.findById(subscriptionId).orElseThrow();
            record.setCustomerId(sub.getCustomer().getId());
            record.setPublicationId((sub.getItems() == null || sub.getItems().isEmpty()) ? null
                    : sub.getItems().get(0).getPublication().getId());
        }

        record.setStatus(status);
        saveDeliveryRecordWithRetry(record);
    }

    /**
     * Generates and saves PENDING delivery records for a specific date.
     * Uses Google Fleet Routing API to optimally assign deliveries to drivers.
     * Falls back to round-robin for any subscriptions missing coordinates.
     */
    public void generateSchedulesForDate(LocalDate date) {
        List<Subscription> allActive = subscriptionRepository.findAll().stream()
                .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE)
                .collect(Collectors.toList());

        List<DeliveryPerson> allDeliveryPersons = deliveryPersonRepository.findAll().stream()
                .filter(p -> "APPROVED".equalsIgnoreCase(p.getStatus()))
                .collect(Collectors.toList());

        if (allDeliveryPersons.isEmpty()) {
            log.info("No approved delivery persons found – skipping schedule generation.");
            return;
        }

        // Filter to only subscriptions that need a record today
        List<Subscription> toSchedule = allActive.stream()
                .filter(sub -> {
                    if (sub.getStartDate() != null && sub.getStartDate().isAfter(date))
                        return false;
                    if (sub.getEndDate() != null && sub.getEndDate().isBefore(date))
                        return false;
                    if (sub.getSuspendStartDate() != null && sub.getSuspendEndDate() != null) {
                        if ((date.isEqual(sub.getSuspendStartDate()) || date.isAfter(sub.getSuspendStartDate())) &&
                                (date.isEqual(sub.getSuspendEndDate()) || date.isBefore(sub.getSuspendEndDate()))) {
                            return false;
                        }
                    }
                    if (sub.getItems() == null || sub.getItems().stream().noneMatch(item -> isItemActive(item, date))) {
                        return false;
                    }
                    return !deliveryRecordRepository.existsByDeliveryDateAndSubscriptionId(date, sub.getId());
                })
                .collect(Collectors.toList());

        if (toSchedule.isEmpty()) {
            log.info("All deliveries already scheduled for " + date);
            return;
        }

        // Split: subscriptions WITH coordinates go to Fleet Routing, rest to
        // round-robin
        List<Map<String, Object>> stops = new ArrayList<>();
        List<Subscription> noCoordSubs = new ArrayList<>();

        for (Subscription sub : toSchedule) {
            Double lat = sub.getCustomerAddress() != null ? sub.getCustomerAddress().getLatitude() : null;
            Double lng = sub.getCustomerAddress() != null ? sub.getCustomerAddress().getLongitude() : null;

            if (lat != null && lng != null) {
                Map<String, Object> stop = new HashMap<>();
                stop.put("subscriptionId", sub.getId().toString());
                stop.put("lat", lat);
                stop.put("lng", lng);
                stops.add(stop);
            } else {
                noCoordSubs.add(sub);
            }
        }

        // ── Fleet Routing assignment ──────────────────────────────────────────
        List<FleetRoutingService.RouteAssignment> assignments = Collections.emptyList();
        if (!stops.isEmpty()) {
            try {
                List<Hub> activeHubs = hubRepository.findAll().stream()
                        .filter(Hub::isActive)
                        .collect(Collectors.toList());

                assignments = fleetRoutingService.optimizeRoutes(stops, allDeliveryPersons.size(), activeHubs);
                log.info("Fleet Routing successfully assigned {} stops across {} hubs.", assignments.size(),
                        activeHubs.size());
            } catch (Exception e) {
                log.error("Fleet Routing API failed – falling back to round-robin. Error: " + e.getMessage(), e);
            }
        }

        // Keep track of which subIds got dynamically assigned
        Set<String> assignedSubIds = new HashSet<>();

        // Save records for Fleet-Routed stops
        int fallbackIdx = 0;
        for (FleetRoutingService.RouteAssignment assignment : assignments) {
            String subIdStr = assignment.getSubscriptionId();
            assignedSubIds.add(subIdStr);
            UUID subId = UUID.fromString(subIdStr);
            Subscription sub = toSchedule.stream()
                    .filter(s -> s.getId().equals(subId)).findFirst().orElse(null);
            if (sub == null)
                continue;

            int vehicleIdx = assignment.getVehicleIndex();
            DeliveryPerson assigned = (vehicleIdx < allDeliveryPersons.size())
                    ? allDeliveryPersons.get(vehicleIdx)
                    : allDeliveryPersons.get(fallbackIdx++ % allDeliveryPersons.size());

            saveRecord(sub, assigned, date, assignment.getAssignedHubId(), assignment.getRouteSequence());
        }

        // Save records for stops without coordinates or skipped by FleetRouting
        // (round-robin fallback)
        for (int i = 0; i < toSchedule.size(); i++) {
            Subscription sub = toSchedule.get(i);
            if (!assignedSubIds.contains(sub.getId().toString())) {
                saveRecord(sub, allDeliveryPersons.get(i % allDeliveryPersons.size()), date, null, null);
            }
        }

        log.info("Schedule generation complete for " + date
                + " – total records: " + (stops.size() + noCoordSubs.size()));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void saveRecord(Subscription sub, DeliveryPerson person, LocalDate date, UUID hubId,
            Integer routeSequence) {
        DeliveryRecord record = DeliveryRecord.builder()
                .deliveryDate(date)
                .deliveryPersonId(person.getId())
                .subscriptionId(sub.getId())
                .customerId(sub.getCustomer().getId())
                .publicationId((sub.getItems() == null || sub.getItems().isEmpty()) ? null
                        : sub.getItems().get(0).getPublication().getId())
                .status(DeliveryRecord.DeliveryStatus.PENDING)
                .hubId(hubId)
                .routeSequence(routeSequence)
                .build();
        saveDeliveryRecordWithRetry(record);
    }

    private void saveDeliveryRecordWithRetry(DeliveryRecord record) {
        try {
            deliveryRecordRepository.save(record);
        } catch (DataIntegrityViolationException ex) {
            if (!isDeliveryRecordPrimaryKeyConflict(ex)) {
                throw ex;
            }
            deliveryRecordRepository.save(record);
        }
    }

    private boolean isDeliveryRecordPrimaryKeyConflict(DataIntegrityViolationException ex) {
        String message = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();
        return message != null && message.contains("delivery_records_pkey");
    }

    private boolean isItemActive(SubscriptionItem item, LocalDate date) {
        if (item.getStatus() == com.naas.backend.subscription.SubscriptionItemStatus.REMOVED) {
            return false;
        }
        if (item.getStatus() == com.naas.backend.subscription.SubscriptionItemStatus.SUSPENDED) {
            if (item.getStopStartDate() != null && item.getStopEndDate() != null) {
                if ((date.isEqual(item.getStopStartDate()) || date.isAfter(item.getStopStartDate())) &&
                        (date.isEqual(item.getStopEndDate()) || date.isBefore(item.getStopEndDate()))) {
                    return false;
                }
            }
        }

        // Frequency Check
        String freq = item.getFrequency() != null ? item.getFrequency().toUpperCase() : "DAILY";

        switch (freq) {
            case "DAILY":
                return true;
            case "WEEKLY":
                // e.g. Deliver on Sunday if WEEKLY
                return date.getDayOfWeek() == java.time.DayOfWeek.SUNDAY;
            case "MONTHLY":
                // e.g. Deliver on the 1st of the month if MONTHLY
                return date.getDayOfMonth() == 1;
            case "ALTERNATE":
                // Simple rule: deliver on even days of counting epoch, or just basic day of
                // year check
                return date.getDayOfYear() % 2 == 0;
            case "CUSTOM":
                String customDays = item.getCustomDeliveryDays();
                if (customDays != null && !customDays.isEmpty()) {
                    String todayName = date.getDayOfWeek().name();
                    return customDays.toUpperCase().contains(todayName);
                }
                return false;
            default:
                return true; // Fallback to daily
        }
    }
}
