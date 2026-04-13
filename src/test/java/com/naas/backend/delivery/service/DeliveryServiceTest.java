package com.naas.backend.delivery.service;

import com.naas.backend.customer.Customer;
import com.naas.backend.customer.CustomerAddress;
import com.naas.backend.customer.CustomerRepository;
import com.naas.backend.delivery.entity.DeliveryRecord;
import com.naas.backend.delivery.repository.DeliveryRecordRepository;
import com.naas.backend.deliveryperson.DeliveryPerson;
import com.naas.backend.deliveryperson.DeliveryPersonRepository;
import com.naas.backend.hub.HubRepository;
import com.naas.backend.publication.Publication;
import com.naas.backend.subscription.Subscription;
import com.naas.backend.subscription.SubscriptionItem;
import com.naas.backend.subscription.SubscriptionItemStatus;
import com.naas.backend.subscription.SubscriptionRepository;
import com.naas.backend.subscription.SubscriptionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for DeliveryService — Feature 5 (FR-DM1)
 *
 * SRS Reference:
 *   FR-DM1: The system shall generate day-by-day delivery lists for delivery
 *            personnel, correctly excluding customers with active stop periods.
 */
@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {

    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private DeliveryRecordRepository deliveryRecordRepository;
    @Mock private DeliveryPersonRepository deliveryPersonRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private HubRepository hubRepository;
    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private FleetRoutingService fleetRoutingService;

    @InjectMocks
    private DeliveryService deliveryService;

    private DeliveryPerson deliveryPerson;
    private Customer customer1;       // Will have an active delivery
    private Customer customer2;       // Will be ON PAUSE — must be excluded
    private CustomerAddress address1;
    private CustomerAddress address2;

    @BeforeEach
    void setUp() {
        deliveryPerson = DeliveryPerson.builder()
                .id(UUID.randomUUID())
                .name("Ramesh")
                .status("APPROVED")
                .build();

        customer1 = Customer.builder().id(UUID.randomUUID()).name("Suresh").active(true).build();
        customer2 = Customer.builder().id(UUID.randomUUID()).name("Ganesh").active(true).build();

        address1 = CustomerAddress.builder()
                .id(UUID.randomUUID())
                .customer(customer1)
                .address("12, Gandhi Street")
                .latitude(13.05).longitude(80.21)
                .label("Home").active(true)
                .build();

        address2 = CustomerAddress.builder()
                .id(UUID.randomUUID())
                .customer(customer2)
                .address("45, Nehru Nagar")
                .latitude(13.07).longitude(80.24)
                .label("Home").active(true)
                .build();
    }

    // ── Helper: builds a basic subscription with one active publication ───────
    private Subscription buildActiveSubscription(Customer customer, CustomerAddress address,
                                                  LocalDate startDate, LocalDate suspendStart,
                                                  LocalDate suspendEnd) {
        Publication pub = new Publication();
        pub.setId(UUID.randomUUID());
        pub.setName("The Hindu");
        pub.setPrice(5.0);

        SubscriptionItem item = SubscriptionItem.builder()
                .id(UUID.randomUUID())
                .publication(pub)
                .status(SubscriptionItemStatus.ACTIVE)
                .build();

        return Subscription.builder()
                .id(UUID.randomUUID())
                .customer(customer)
                .customerAddress(address)
                .status(SubscriptionStatus.ACTIVE)
                .startDate(startDate)
                .suspendStartDate(suspendStart)
                .suspendEndDate(suspendEnd)
                .items(new ArrayList<>(List.of(item)))
                .build();
    }

    // ── FR-DM1: Normal deliveries are scheduled ───────────────────────────────

    @Test
    @DisplayName("FR-DM1: Active subscription with no pause should generate a delivery record")
    void generateSchedulesForDate_shouldCreateRecord_forSubscriptionWithNoActivePause() throws Exception {
        // Arrange — customer1 has an active subscription, no suspension
        LocalDate today = LocalDate.now();
        Subscription sub = buildActiveSubscription(customer1, address1,
                today.minusMonths(1), null, null); // No pause dates set

        when(subscriptionRepository.findAll()).thenReturn(List.of(sub));
        when(deliveryPersonRepository.findAll()).thenReturn(List.of(deliveryPerson));
        when(deliveryRecordRepository.existsByDeliveryDateAndSubscriptionId(today, sub.getId()))
                .thenReturn(false); // Not yet scheduled
        when(hubRepository.findAll()).thenReturn(List.of());
        when(fleetRoutingService.optimizeRoutes(any(), anyInt(), any()))
                .thenThrow(new RuntimeException("No hubs — fall back to round-robin"));

        // Act
        deliveryService.generateSchedulesForDate(today);

        // Assert — one delivery record was created (round-robin fallback)
        verify(deliveryRecordRepository, atLeastOnce()).save(any(DeliveryRecord.class));
    }

    // ── FR-DM1: Suspended subscriptions are excluded ──────────────────────────

    @Test
    @DisplayName("FR-DM1: Subscription suspended TODAY should be EXCLUDED from delivery schedule")
    void generateSchedulesForDate_shouldExclude_subscriptionSuspendedAsOfToday() throws Exception {
        // Arrange — customer2's subscription is suspended starting today for 5 days
        LocalDate today = LocalDate.now();
        Subscription pausedSub = buildActiveSubscription(customer2, address2,
                today.minusMonths(1),
                today,              // suspendStartDate = TODAY (on pause right now)
                today.plusDays(5)); // suspendEndDate = 5 days from now

        // customer1 has a normal active subscription
        Subscription activeSub = buildActiveSubscription(customer1, address1,
                today.minusMonths(1), null, null);

        when(subscriptionRepository.findAll()).thenReturn(List.of(pausedSub, activeSub));
        when(deliveryPersonRepository.findAll()).thenReturn(List.of(deliveryPerson));
        when(deliveryRecordRepository.existsByDeliveryDateAndSubscriptionId(eq(today), eq(activeSub.getId())))
                .thenReturn(false);
        // The paused sub: existsByDelivery... should never be reached as it's filtered first
        when(hubRepository.findAll()).thenReturn(List.of());
        when(fleetRoutingService.optimizeRoutes(any(), anyInt(), any()))
                .thenThrow(new RuntimeException("fallback"));

        // Act
        deliveryService.generateSchedulesForDate(today);

        // Assert — only 1 record saved (the active one), the paused sub generates nothing
        ArgumentCaptor<DeliveryRecord> captor = ArgumentCaptor.forClass(DeliveryRecord.class);
        verify(deliveryRecordRepository, times(1)).save(captor.capture());
        // The saved delivery record must be for customer1 (not the paused customer2)
        assertThat(captor.getValue().getCustomerId()).isEqualTo(customer1.getId());
    }

    @Test
    @DisplayName("FR-DM1: Subscription that starts in the FUTURE should be excluded from today's schedule")
    void generateSchedulesForDate_shouldExclude_subscriptionWithFutureStartDate() {
        // Arrange — a subscription that doesn't start until next week
        LocalDate today = LocalDate.now();
        Subscription futureSubscription = buildActiveSubscription(customer1, address1,
                today.plusDays(7), // starts next week — not yet active
                null, null);

        when(subscriptionRepository.findAll()).thenReturn(List.of(futureSubscription));
        when(deliveryPersonRepository.findAll()).thenReturn(List.of(deliveryPerson));

        // Act
        deliveryService.generateSchedulesForDate(today);

        // Assert — no delivery records created, delivery is not due yet
        verify(deliveryRecordRepository, never()).save(any(DeliveryRecord.class));
    }

    @Test
    @DisplayName("FR-DM1: A subscription that already has a record for today should not create a duplicate")
    void generateSchedulesForDate_shouldNotCreateDuplicate_whenRecordAlreadyExistsForDate() {
        // Arrange — delivery already scheduled for this subscription today
        LocalDate today = LocalDate.now();
        Subscription sub = buildActiveSubscription(customer1, address1,
                today.minusMonths(1), null, null);

        when(subscriptionRepository.findAll()).thenReturn(List.of(sub));
        when(deliveryPersonRepository.findAll()).thenReturn(List.of(deliveryPerson));
        when(deliveryRecordRepository.existsByDeliveryDateAndSubscriptionId(today, sub.getId()))
                .thenReturn(true); // Already scheduled!

        // Act
        deliveryService.generateSchedulesForDate(today);

        // Assert — save must NOT be called again (no duplicate records)
        verify(deliveryRecordRepository, never()).save(any(DeliveryRecord.class));
    }

    @Test
    @DisplayName("FR-DM1: If no approved delivery persons exist, no schedule should be generated")
    void generateSchedulesForDate_shouldDoNothing_whenNoApprovedDeliveryPersonsExist() {
        // Arrange — all delivery persons are PENDING approval
        LocalDate today = LocalDate.now();
        DeliveryPerson pendingPerson = DeliveryPerson.builder()
                .id(UUID.randomUUID())
                .name("New Guy")
                .status("PENDING") // Not yet approved
                .build();

        Subscription sub = buildActiveSubscription(customer1, address1,
                today.minusMonths(1), null, null);

        when(subscriptionRepository.findAll()).thenReturn(List.of(sub));
        when(deliveryPersonRepository.findAll()).thenReturn(List.of(pendingPerson));

        // Act
        deliveryService.generateSchedulesForDate(today);

        // Assert — no records should be saved; no approved DP available
        verify(deliveryRecordRepository, never()).save(any(DeliveryRecord.class));
    }
}
