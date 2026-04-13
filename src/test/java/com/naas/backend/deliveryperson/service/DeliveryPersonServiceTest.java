package com.naas.backend.deliveryperson.service;

import com.naas.backend.auth.entity.User;
import com.naas.backend.auth.repository.UserRepository;
import com.naas.backend.delivery.entity.DeliveryRecord;
import com.naas.backend.delivery.repository.DeliveryRecordRepository;
import com.naas.backend.deliveryperson.DeliveryPerson;
import com.naas.backend.deliveryperson.DeliveryPersonRepository;
import com.naas.backend.publication.Publication;
import com.naas.backend.publication.PublicationRepository;
import com.naas.backend.subscription.Subscription;
import com.naas.backend.subscription.SubscriptionItem;
import com.naas.backend.subscription.SubscriptionItemStatus;
import com.naas.backend.subscription.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for DeliveryPersonService — Feature 9 (FR-DPM7)
 *
 * SRS Reference:
 *   FR-DPM7: The system shall calculate payout for delivery personnel based on
 *             2.5% of the total value of publications delivered during the
 *             payment period.
 */
@ExtendWith(MockitoExtension.class)
class DeliveryPersonServiceTest {

    @Mock private DeliveryPersonRepository deliveryPersonRepository;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private DeliveryRecordRepository deliveryRecordRepository;
    @Mock private PublicationRepository publicationRepository;
    @Mock private SubscriptionRepository subscriptionRepository;

    @InjectMocks
    private DeliveryPersonService deliveryPersonService;

    private UUID deliveryPersonId;
    private DeliveryPerson deliveryPerson;
    private LocalDate periodStart;
    private LocalDate periodEnd;

    @BeforeEach
    void setUp() {
        deliveryPersonId = UUID.randomUUID();

        deliveryPerson = DeliveryPerson.builder()
                .id(deliveryPersonId)
                .name("Raju Delivery")
                .status("APPROVED")
                .build();

        // March 2026 as the payout period
        periodStart = LocalDate.of(2026, 3, 1);
        periodEnd = LocalDate.of(2026, 3, 31);
    }

    // ── Helper: builds a DELIVERED record linked to a subscription ────────────
    private DeliveryRecord buildDeliveredRecord(UUID subId, LocalDate date) {
        return DeliveryRecord.builder()
                .id(UUID.randomUUID())
                .deliveryPersonId(deliveryPersonId)
                .subscriptionId(subId)
                .deliveryDate(date)
                .status(DeliveryRecord.DeliveryStatus.DELIVERED)
                .build();
    }

    // ── Helper: builds a subscription with publication of given price ──────────
    private Subscription buildSubscriptionWithPrice(double price) {
        Publication pub = new Publication();
        pub.setId(UUID.randomUUID());
        pub.setName("Test Paper");
        pub.setPrice(price);

        SubscriptionItem item = SubscriptionItem.builder()
                .id(UUID.randomUUID())
                .publication(pub)
                .status(SubscriptionItemStatus.ACTIVE)
                .build();

        return Subscription.builder()
                .id(UUID.randomUUID())
                .status(com.naas.backend.subscription.SubscriptionStatus.ACTIVE)
                .items(new ArrayList<>(List.of(item)))
                .build();
    }

    // ── FR-DPM7: 2.5% Commission Rule ────────────────────────────────────────

    @Test
    @DisplayName("FR-DPM7: Payout on ₹10,000 total delivered value should be exactly ₹250 (2.5%)")
    void calculatePayout_shouldReturn250_whenTotalDeliveredValueIs10000() {
        // Arrange — 4 deliveries, each subscription worth ₹2500 (total = ₹10,000)
        // Because SubscriptionItem price = ₹2500, and 4 DELIVERED records = 4 × ₹2500 = ₹10,000
        Subscription sub = buildSubscriptionWithPrice(2500.0);
        UUID subId = sub.getId();

        List<DeliveryRecord> deliveries = List.of(
                buildDeliveredRecord(subId, LocalDate.of(2026, 3, 1)),
                buildDeliveredRecord(subId, LocalDate.of(2026, 3, 8)),
                buildDeliveredRecord(subId, LocalDate.of(2026, 3, 15)),
                buildDeliveredRecord(subId, LocalDate.of(2026, 3, 22))
        );

        when(deliveryRecordRepository.findByDeliveryPersonIdAndDeliveryDateBetweenAndStatus(
                eq(deliveryPersonId), eq(periodStart), eq(periodEnd),
                eq(DeliveryRecord.DeliveryStatus.DELIVERED)))
                .thenReturn(deliveries);
        when(subscriptionRepository.findById(subId)).thenReturn(Optional.of(sub));

        // Act
        Double payout = deliveryPersonService.calculatePayout(deliveryPersonId, periodStart, periodEnd);

        // Assert — 2.5% of ₹10,000 = ₹250.00
        // 4 deliveries × 1 item × ₹2500 = ₹10,000 total value
        assertThat(payout).isEqualTo(250.0);
    }

    @Test
    @DisplayName("FR-DPM7: Payout with multiple publications per delivery (2 newspapers, different prices)")
    void calculatePayout_shouldSumAllPublicationPrices_perDelivery() {
        // Arrange — subscription with 2 publications: The Hindu (₹5) + TOI (₹10)
        // 1 delivery = ₹5 + ₹10 = ₹15; 2.5% of ₹15 = ₹0.375
        Publication hindu = new Publication();
        hindu.setId(UUID.randomUUID());
        hindu.setName("The Hindu");
        hindu.setPrice(5.0);

        Publication toi = new Publication();
        toi.setId(UUID.randomUUID());
        toi.setName("Times of India");
        toi.setPrice(10.0);

        SubscriptionItem item1 = SubscriptionItem.builder()
                .id(UUID.randomUUID()).publication(hindu)
                .status(SubscriptionItemStatus.ACTIVE).build();
        SubscriptionItem item2 = SubscriptionItem.builder()
                .id(UUID.randomUUID()).publication(toi)
                .status(SubscriptionItemStatus.ACTIVE).build();

        Subscription multiPubSub = Subscription.builder()
                .id(UUID.randomUUID())
                .status(com.naas.backend.subscription.SubscriptionStatus.ACTIVE)
                .items(new ArrayList<>(List.of(item1, item2)))
                .build();

        List<DeliveryRecord> deliveries = List.of(
                buildDeliveredRecord(multiPubSub.getId(), LocalDate.of(2026, 3, 1))
        );

        when(deliveryRecordRepository.findByDeliveryPersonIdAndDeliveryDateBetweenAndStatus(
                eq(deliveryPersonId), eq(periodStart), eq(periodEnd),
                eq(DeliveryRecord.DeliveryStatus.DELIVERED)))
                .thenReturn(deliveries);
        when(subscriptionRepository.findById(multiPubSub.getId()))
                .thenReturn(Optional.of(multiPubSub));

        // Act
        Double payout = deliveryPersonService.calculatePayout(deliveryPersonId, periodStart, periodEnd);

        // Assert — total value = ₹5 + ₹10 = ₹15; 2.5% of ₹15 = ₹0.375
        assertThat(payout).isCloseTo(0.375, within(0.001));
    }

    @Test
    @DisplayName("FR-DPM7: Zero deliveries in period should result in zero payout")
    void calculatePayout_shouldReturnZero_whenNoDeliveriesInPeriod() {
        // Arrange — delivery person made no deliveries this month
        when(deliveryRecordRepository.findByDeliveryPersonIdAndDeliveryDateBetweenAndStatus(
                eq(deliveryPersonId), eq(periodStart), eq(periodEnd),
                eq(DeliveryRecord.DeliveryStatus.DELIVERED)))
                .thenReturn(List.of()); // Empty list — zero deliveries!

        // Act
        Double payout = deliveryPersonService.calculatePayout(deliveryPersonId, periodStart, periodEnd);

        // Assert — payout must be exactly ₹0 (no division by zero, no NullPointerException)
        assertThat(payout).isEqualTo(0.0);
    }

    @Test
    @DisplayName("FR-DPM7: Only DELIVERED records should count — PENDING/CANCELLED records are excluded")
    void calculatePayout_shouldOnlyCount_deliveredStatusRecords() {
        // Arrange — this test verifies the repository is queried with the DELIVERED status filter
        when(deliveryRecordRepository.findByDeliveryPersonIdAndDeliveryDateBetweenAndStatus(
                eq(deliveryPersonId), eq(periodStart), eq(periodEnd),
                eq(DeliveryRecord.DeliveryStatus.DELIVERED)))
                .thenReturn(List.of()); // Even if there are PENDING records, they won't be here

        // Act
        Double payout = deliveryPersonService.calculatePayout(deliveryPersonId, periodStart, periodEnd);

        // Assert — correct status filter was passed to repository
        verify(deliveryRecordRepository).findByDeliveryPersonIdAndDeliveryDateBetweenAndStatus(
                eq(deliveryPersonId), eq(periodStart), eq(periodEnd),
                eq(DeliveryRecord.DeliveryStatus.DELIVERED)
        );
        assertThat(payout).isEqualTo(0.0);
    }

    @Test
    @DisplayName("FR-DPM7: Payout for a single day worth ₹5.00 should be exactly ₹0.125 (2.5%)")
    void calculatePayout_shouldReturn0Point125_forSingleDeliveryWorthFiveRupees() {
        // Arrange — one delivery worth ₹5.00
        Subscription sub = buildSubscriptionWithPrice(5.0);
        List<DeliveryRecord> deliveries = List.of(
                buildDeliveredRecord(sub.getId(), LocalDate.of(2026, 3, 10))
        );

        when(deliveryRecordRepository.findByDeliveryPersonIdAndDeliveryDateBetweenAndStatus(
                any(), any(), any(), eq(DeliveryRecord.DeliveryStatus.DELIVERED)))
                .thenReturn(deliveries);
        when(subscriptionRepository.findById(sub.getId())).thenReturn(Optional.of(sub));

        // Act
        Double payout = deliveryPersonService.calculatePayout(deliveryPersonId, periodStart, periodEnd);

        // Assert — 2.5% of ₹5.00 = ₹0.125
        assertThat(payout).isCloseTo(0.125, within(0.0001));
    }

    // ── FR-DPM1 / FR-DPM2: Application approval workflow ────────────────────

    @Test
    @DisplayName("FR-DPM1: Approving a delivery person should set status to APPROVED and enable their account")
    void approveDeliveryPerson_shouldSetStatusApproved_andActivateUser() {
        // Arrange — a PENDING delivery person applicant
        User pendingUser = User.builder()
                .id(UUID.randomUUID())
                .email("newguy@test.com")
                .role(User.Role.DELIVERY_PERSON)
                .active(false) // Account disabled until approved
                .build();

        DeliveryPerson pending = DeliveryPerson.builder()
                .id(deliveryPersonId)
                .user(pendingUser)
                .name("New Guy")
                .status("PENDING")
                .build();

        when(deliveryPersonRepository.findById(deliveryPersonId)).thenReturn(Optional.of(pending));
        when(userRepository.save(any(User.class))).thenReturn(pendingUser);
        when(deliveryPersonRepository.save(any(DeliveryPerson.class))).thenReturn(pending);

        // Act
        deliveryPersonService.approveDeliveryPerson(deliveryPersonId);

        // Assert — status becomes APPROVED and user account becomes active
        ArgumentCaptor<DeliveryPerson> dpCaptor = ArgumentCaptor.forClass(DeliveryPerson.class);
        verify(deliveryPersonRepository).save(dpCaptor.capture());
        assertThat(dpCaptor.getValue().getStatus()).isEqualTo("APPROVED");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().isEnabled()).isTrue();
    }

    @Test
    @DisplayName("FR-DPM4: Rejecting a delivery person should set their status to REJECTED")
    void rejectDeliveryPerson_shouldSetStatusRejected() {
        // Arrange
        DeliveryPerson pending = DeliveryPerson.builder()
                .id(deliveryPersonId)
                .name("Rejected Guy")
                .status("PENDING")
                .build();

        when(deliveryPersonRepository.findById(deliveryPersonId)).thenReturn(Optional.of(pending));
        when(deliveryPersonRepository.save(any())).thenReturn(pending);

        // Act
        deliveryPersonService.rejectDeliveryPerson(deliveryPersonId);

        // Assert
        ArgumentCaptor<DeliveryPerson> captor = ArgumentCaptor.forClass(DeliveryPerson.class);
        verify(deliveryPersonRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("REJECTED");
    }
}
