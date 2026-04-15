package com.naas.backend.subscription;

import java.util.UUID;

import com.naas.backend.customer.Customer;
import com.naas.backend.customer.CustomerRepository;
import com.naas.backend.publication.Publication;
import com.naas.backend.publication.PublicationRepository;
import com.naas.backend.subscription.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

        private final SubscriptionRepository subscriptionRepository;
        private final GlobalDeliveryPauseRepository globalDeliveryPauseRepository;
        private final CustomerRepository customerRepository;
        private final PublicationRepository publicationRepository;
        private final com.naas.backend.customer.CustomerAddressRepository customerAddressRepository;

        private static final int ADVANCE_NOTICE_DAYS = 7;

        private void validateAdvanceNotice(LocalDate desiredDate) {
                LocalDate minAllowedDate = LocalDate.now().plusDays(ADVANCE_NOTICE_DAYS);
                if (desiredDate.isBefore(minAllowedDate)) {
                        throw new IllegalArgumentException("Action requires at least " + ADVANCE_NOTICE_DAYS
                                        + " days advance notice. Earliest allowed date is " + minAllowedDate);
                }
        }

        public List<SubscriptionResponse> createSubscription(UUID customerId, CreateSubscriptionRequest request) {
                validateAdvanceNotice(request.getStartDate());

                Customer customer = customerRepository.findById(customerId)
                                .orElseThrow(() -> new RuntimeException("Customer not found"));

                if (request.getItems() == null || request.getItems().isEmpty()) {
                        throw new IllegalArgumentException("At least one publication must be selected");
                }

                if (request.getAddressId() == null) {
                        throw new IllegalArgumentException("Delivery address must be selected");
                }

                com.naas.backend.customer.CustomerAddress customerAddress = customerAddressRepository
                                .findById(request.getAddressId())
                                .orElseThrow(() -> new IllegalArgumentException("Delivery address not found"));

                if (!customerAddress.getCustomer().getId().equals(customerId)) {
                        throw new IllegalArgumentException("Delivery address does not belong to customer");
                }

                List<Subscription> existingSubscriptions = subscriptionRepository.findByCustomerIdAndStatus(customerId,
                                SubscriptionStatus.ACTIVE);

                Subscription subscription = Subscription.builder()
                                .customer(customer)
                                .customerAddress(customerAddress)
                                .startDate(request.getStartDate())
                                .status(SubscriptionStatus.ACTIVE)
                                .build();

                java.util.List<SubscriptionItem> requestedItems = new java.util.ArrayList<>();

                for (CreateSubscriptionRequest.ItemRequest itemReq : request.getItems()) {
                        UUID pubId = itemReq.getPublicationId();
                        if (pubId == null) {
                                throw new IllegalArgumentException("Publication ID cannot be null");
                        }
                        Publication publication = publicationRepository.findById(pubId)
                                        .orElseThrow(() -> new RuntimeException(
                                                        "Publication not found with id: " + pubId));

                        String targetFrequency = "DAILY";
                        if (itemReq.getFrequency() != null && !itemReq.getFrequency().isEmpty()) {
                                targetFrequency = itemReq.getFrequency();
                        } else if (publication.getFrequency() != null && !publication.getFrequency().isEmpty()) {
                                targetFrequency = publication.getFrequency();
                        }

                        SubscriptionItem item = SubscriptionItem.builder()
                                        .subscription(subscription)
                                        .publication(publication)
                                        .frequency(targetFrequency.toUpperCase())
                                        .customDeliveryDays(itemReq.getCustomDeliveryDays())
                                        .build();

                        requestedItems.add(item);
                }

                subscription.setItems(requestedItems);

                Subscription savedSubscription = subscriptionRepository.save(subscription);

                return List.of(mapToResponse(savedSubscription));
        }

        public SubscriptionResponse cancelSubscription(UUID customerId, UUID subscriptionId,
                        CancelSubscriptionRequest request) {
                if (request.getCancelDate() == null) {
                        request.setCancelDate(LocalDate.now().plusDays(ADVANCE_NOTICE_DAYS));
                } else {
                        validateAdvanceNotice(request.getCancelDate());
                }

                Subscription subscription = subscriptionRepository.findByIdAndCustomerId(subscriptionId, customerId)
                                .orElseThrow(() -> new RuntimeException("Subscription not found"));

                subscription.setStatus(SubscriptionStatus.CANCELLED);
                subscription.setEndDate(request.getCancelDate());

                return mapToResponse(subscriptionRepository.save(subscription));
        }

        public SubscriptionResponse suspendSubscription(UUID customerId, UUID subscriptionId,
                        SuspendSubscriptionRequest request) {
                validateAdvanceNotice(request.getSuspendStartDate());
                if (request.getSuspendEndDate() != null
                                && request.getSuspendEndDate().isBefore(request.getSuspendStartDate())) {
                        throw new IllegalArgumentException("Suspend end date cannot be before start date");
                }

                Subscription subscription = subscriptionRepository.findByIdAndCustomerId(subscriptionId, customerId)
                                .orElseThrow(() -> new RuntimeException("Subscription not found"));

                subscription.setSuspendStartDate(request.getSuspendStartDate());
                subscription.setSuspendEndDate(request.getSuspendEndDate());

                return mapToResponse(subscriptionRepository.save(subscription));
        }

        public SubscriptionResponse removeSubscriptionSuspension(UUID customerId, UUID subscriptionId) {
                Subscription subscription = subscriptionRepository.findByIdAndCustomerId(subscriptionId, customerId)
                                .orElseThrow(() -> new RuntimeException("Subscription not found"));

                subscription.setSuspendStartDate(null);
                subscription.setSuspendEndDate(null);

                return mapToResponse(subscriptionRepository.save(subscription));
        }

        public SubscriptionResponse changeSubscriptionAddress(UUID customerId, UUID subscriptionId,
                        ChangeAddressRequest request) {
                Subscription subscription = subscriptionRepository.findByIdAndCustomerId(subscriptionId, customerId)
                                .orElseThrow(() -> new RuntimeException("Subscription not found"));

                com.naas.backend.customer.CustomerAddress address = customerAddressRepository
                                .findById(request.getAddressId())
                                .orElseThrow(() -> new RuntimeException("Address not found"));

                if (!address.getCustomer().getId().equals(customerId)) {
                        throw new RuntimeException("Address does not belong to customer");
                }

                subscription.setCustomerAddress(address);
                return mapToResponse(subscriptionRepository.save(subscription));
        }

        public SubscriptionResponse removeSubscriptionItemSuspension(UUID customerId, UUID subscriptionId,
                        UUID itemId) {
                Subscription subscription = subscriptionRepository.findByIdAndCustomerId(subscriptionId, customerId)
                                .orElseThrow(() -> new RuntimeException("Subscription not found"));

                SubscriptionItem item = subscription.getItems().stream()
                                .filter(i -> i.getId().equals(itemId))
                                .findFirst()
                                .orElseThrow(() -> new RuntimeException("Subscription item not found"));

                item.setStopStartDate(null);
                item.setStopEndDate(null);

                if (item.getStatus() == SubscriptionItemStatus.SUSPENDED) {
                        item.setStatus(SubscriptionItemStatus.ACTIVE);
                }

                return mapToResponse(subscriptionRepository.save(subscription));
        }

        public SubscriptionResponse updateItemStatus(UUID customerId, UUID subscriptionId, UUID itemId,
                        ItemStatusRequest request) {
                Subscription subscription = subscriptionRepository.findByIdAndCustomerId(subscriptionId, customerId)
                                .orElseThrow(() -> new RuntimeException("Subscription not found"));

                SubscriptionItem item = subscription.getItems().stream()
                                .filter(i -> i.getId().equals(itemId))
                                .findFirst()
                                .orElseThrow(() -> new RuntimeException("Subscription item not found"));

                if (request.getStatus() == SubscriptionItemStatus.REMOVED
                                || request.getStatus() == SubscriptionItemStatus.SUSPENDED) {
                        LocalDate checkDate = request.getStopStartDate() != null ? request.getStopStartDate()
                                        : LocalDate.now().plusDays(ADVANCE_NOTICE_DAYS);
                        validateAdvanceNotice(checkDate);
                }

                if (request.getStatus() != null) {
                        item.setStatus(request.getStatus());
                }
                if (request.getStopStartDate() != null) {
                        item.setStopStartDate(request.getStopStartDate());
                }
                if (request.getStopEndDate() != null) {
                        item.setStopEndDate(request.getStopEndDate());
                }

                return mapToResponse(subscriptionRepository.save(subscription));
        }

        public GlobalDeliveryPauseResponse addGlobalPause(UUID customerId, GlobalDeliveryPauseRequest request) {
                validateAdvanceNotice(request.getStartDate());
                if (request.getEndDate() != null && request.getEndDate().isBefore(request.getStartDate())) {
                        throw new IllegalArgumentException("Pause end date cannot be before start date");
                }

                Customer customer = customerRepository.findById(customerId)
                                .orElseThrow(() -> new RuntimeException("Customer not found"));

                GlobalDeliveryPause pause = GlobalDeliveryPause.builder()
                                .customer(customer)
                                .startDate(request.getStartDate())
                                .endDate(request.getEndDate())
                                .build();

                pause = globalDeliveryPauseRepository.save(pause);

                return GlobalDeliveryPauseResponse.builder()
                                .id(pause.getId())
                                .startDate(pause.getStartDate())
                                .endDate(pause.getEndDate())
                                .createdAt(pause.getCreatedAt())
                                .build();
        }

        public List<SubscriptionResponse> getCustomerSubscriptions(UUID customerId) {
                return subscriptionRepository.findByCustomerId(customerId).stream()
                                .map(this::mapToResponse)
                                .collect(Collectors.toList());
        }

        public List<SubscriptionResponse> getAllSubscriptions() {
                return subscriptionRepository.findAll().stream()
                                .map(this::mapToResponse)
                                .collect(Collectors.toList());
        }

        public List<GlobalDeliveryPauseResponse> getCustomerGlobalPauses(UUID customerId) {
                return globalDeliveryPauseRepository.findByCustomerId(customerId).stream()
                                .map(pause -> GlobalDeliveryPauseResponse.builder()
                                                .id(pause.getId())
                                                .startDate(pause.getStartDate())
                                                .endDate(pause.getEndDate())
                                                .createdAt(pause.getCreatedAt())
                                                .build())
                                .collect(Collectors.toList());
        }

        private SubscriptionResponse mapToResponse(Subscription subscription) {
                UUID firstPubId = subscription.getItems() == null || subscription.getItems().isEmpty() ? null
                                : subscription.getItems().get(0).getPublication().getId();
                String pubNames = subscription.getItems() == null || subscription.getItems().isEmpty() ? ""
                                : subscription.getItems().stream()
                                                .map(item -> item.getPublication().getName())
                                                .collect(Collectors.joining(", "));

                List<SubscriptionItemResponse> itemResponses = subscription.getItems() == null
                                ? java.util.Collections.<SubscriptionItemResponse>emptyList()
                                : subscription.getItems().stream().map(item -> SubscriptionItemResponse.builder()
                                                .id(item.getId())
                                                .publicationId(item.getPublication().getId())
                                                .publicationName(item.getPublication().getName())
                                                .price(item.getPublication().getPrice())
                                                .type(item.getPublication().getType() != null
                                                                ? item.getPublication().getType().name()
                                                                : null)
                                                .frequency(item.getFrequency())
                                                .customDeliveryDays(item.getCustomDeliveryDays())
                                                .status(item.getStatus())
                                                .stopStartDate(item.getStopStartDate())
                                                .stopEndDate(item.getStopEndDate())
                                                .build()).collect(Collectors.toList());

                return SubscriptionResponse.builder()
                                .id(subscription.getId())
                                .customerId(subscription.getCustomer() != null ? subscription.getCustomer().getId()
                                                : null)
                                .customerName(subscription.getCustomer() != null ? subscription.getCustomer().getName()
                                                : null)
                                .addressId(subscription.getCustomerAddress() != null
                                                ? subscription.getCustomerAddress().getId()
                                                : null)
                                .address(subscription.getCustomerAddress() != null
                                                ? subscription.getCustomerAddress().getAddress()
                                                : null)
                                .publicationId(firstPubId)
                                .publicationName(pubNames)
                                .items(itemResponses)
                                .status(subscription.getStatus())
                                .startDate(subscription.getStartDate())
                                .endDate(subscription.getEndDate())
                                .suspendStartDate(subscription.getSuspendStartDate())
                                .suspendEndDate(subscription.getSuspendEndDate())
                                .createdAt(subscription.getCreatedAt())
                                .build();
        }
}
