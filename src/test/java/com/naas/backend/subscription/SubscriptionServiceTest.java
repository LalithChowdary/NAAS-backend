package com.naas.backend.subscription;

import com.naas.backend.customer.Customer;
import com.naas.backend.customer.CustomerAddressRepository;
import com.naas.backend.customer.CustomerRepository;
import com.naas.backend.publication.PublicationRepository;
import com.naas.backend.subscription.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for SubscriptionService — Features 2 & 3 (FR-SM6, FR-SM4, FR-SM8)
 *
 * SRS References:
 *   FR-SM6: The system shall enforce a one-week advance notice requirement for
 *            subscription changes, as per agency policy.
 *   FR-SM4: The system shall allow customers to suspend a subscription for a
 *            defined period.
 *   FR-SM8: The system shall allow customers to stop all deliveries for a
 *            specified date range.
 */
@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private GlobalDeliveryPauseRepository globalDeliveryPauseRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private PublicationRepository publicationRepository;
    @Mock private CustomerAddressRepository customerAddressRepository;

    @InjectMocks
    private SubscriptionService subscriptionService;

    private UUID customerId;
    private UUID subscriptionId;
    private Customer customer;
    private Subscription existingSubscription;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        subscriptionId = UUID.randomUUID();

        customer = Customer.builder()
                .id(customerId)
                .name("Test Customer")
                .phone("9999999999")
                .active(true)
                .build();

        existingSubscription = Subscription.builder()
                .id(subscriptionId)
                .customer(customer)
                .status(SubscriptionStatus.ACTIVE)
                .startDate(LocalDate.now().minusMonths(1))
                .build();
    }

    // ── FR-SM6: 7-Day Advance Notice Rule ────────────────────────────────────

    @Test
    @DisplayName("FR-SM6: Cancelling subscription effective TOMORROW should be rejected (< 7 days notice)")
    void cancelSubscription_shouldThrowException_whenCancelDateIsLessThan7DaysAway() {
        // Arrange — requesting cancellation effective tomorrow (clearly < 7 days)
        CancelSubscriptionRequest request = new CancelSubscriptionRequest();
        request.setCancelDate(LocalDate.now().plusDays(1)); // Only 1 day notice!

        // Act & Assert
        assertThatThrownBy(() ->
                subscriptionService.cancelSubscription(customerId, subscriptionId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("7 days advance notice");
    }

    @Test
    @DisplayName("FR-SM6: Cancelling subscription effective in exactly 7 days should be accepted")
    void cancelSubscription_shouldSucceed_whenCancelDateIsExactly7DaysAway() {
        // Arrange — requesting cancellation effective in exactly 7 days (minimum allowed)
        CancelSubscriptionRequest request = new CancelSubscriptionRequest();
        request.setCancelDate(LocalDate.now().plusDays(7));

        when(subscriptionRepository.findByIdAndCustomerId(subscriptionId, customerId))
                .thenReturn(Optional.of(existingSubscription));
        when(subscriptionRepository.save(any(Subscription.class)))
                .thenReturn(existingSubscription);

        // Act — should NOT throw
        subscriptionService.cancelSubscription(customerId, subscriptionId, request);

        // Assert — subscription was saved with new CANCELLED status
        ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
    }

    @Test
    @DisplayName("FR-SM6: Creating new subscription starting in 3 days should be rejected")
    void createSubscription_shouldThrowException_whenStartDateIsLessThan7DaysAway() {
        // Arrange — a new subscription request starting too soon
        CreateSubscriptionRequest request = new CreateSubscriptionRequest();
        request.setStartDate(LocalDate.now().plusDays(3)); // Only 3 days notice
        request.setAddressId(UUID.randomUUID());
        CreateSubscriptionRequest.ItemRequest itemReq = new CreateSubscriptionRequest.ItemRequest();
        itemReq.setPublicationId(UUID.randomUUID());
        request.setItems(java.util.List.of(itemReq));

        // Act & Assert
        assertThatThrownBy(() ->
                subscriptionService.createSubscription(customerId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("7 days advance notice");
    }

    @Test
    @DisplayName("FR-SM6: Suspending subscription with start date tomorrow should be rejected")
    void suspendSubscription_shouldThrowException_whenSuspendStartIsLessThan7DaysAway() {
        // Arrange — trying to suspend starting tomorrow
        SuspendSubscriptionRequest request = new SuspendSubscriptionRequest();
        request.setSuspendStartDate(LocalDate.now().plusDays(2)); // Too soon
        request.setSuspendEndDate(LocalDate.now().plusDays(10));

        // Act & Assert
        assertThatThrownBy(() ->
                subscriptionService.suspendSubscription(customerId, subscriptionId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("7 days advance notice");
    }

    // ── FR-SM4: Subscription-level Suspension ────────────────────────────────

    @Test
    @DisplayName("FR-SM4: Suspending subscription with valid dates (>= 7 days) should save correctly")
    void suspendSubscription_shouldSaveSuspendDates_whenAdvanceNoticeIsSatisfied() {
        // Arrange — valid suspension request (>= 7 days out)
        LocalDate suspendStart = LocalDate.now().plusDays(10);
        LocalDate suspendEnd = LocalDate.now().plusDays(20);

        SuspendSubscriptionRequest request = new SuspendSubscriptionRequest();
        request.setSuspendStartDate(suspendStart);
        request.setSuspendEndDate(suspendEnd);

        when(subscriptionRepository.findByIdAndCustomerId(subscriptionId, customerId))
                .thenReturn(Optional.of(existingSubscription));
        when(subscriptionRepository.save(any(Subscription.class)))
                .thenReturn(existingSubscription);

        // Act
        subscriptionService.suspendSubscription(customerId, subscriptionId, request);

        // Assert — the saved subscription has the correct suspend dates
        ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository).save(captor.capture());
        Subscription saved = captor.getValue();
        assertThat(saved.getSuspendStartDate()).isEqualTo(suspendStart);
        assertThat(saved.getSuspendEndDate()).isEqualTo(suspendEnd);
    }

    @Test
    @DisplayName("FR-SM4: Suspend end date before start date must be rejected")
    void suspendSubscription_shouldReject_whenEndDateIsBeforeStartDate() {
        // Arrange — logically invalid date range
        SuspendSubscriptionRequest request = new SuspendSubscriptionRequest();
        request.setSuspendStartDate(LocalDate.now().plusDays(10));
        request.setSuspendEndDate(LocalDate.now().plusDays(8)); // End is BEFORE start!

        // Act & Assert
        assertThatThrownBy(() ->
                subscriptionService.suspendSubscription(customerId, subscriptionId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("end date cannot be before start date");
    }

    // ── FR-SM8: Global Stop (Customer Out-of-Station) ────────────────────────

    @Test
    @DisplayName("FR-SM8: Adding a valid global delivery pause should save with correct dates")
    void addGlobalPause_shouldSavePauseRecord_whenDatesAreValid() {
        // Arrange — customer is going out of station in 10 days
        LocalDate pauseStart = LocalDate.now().plusDays(10);
        LocalDate pauseEnd = LocalDate.now().plusDays(20);

        GlobalDeliveryPauseRequest request = new GlobalDeliveryPauseRequest();
        request.setStartDate(pauseStart);
        request.setEndDate(pauseEnd);

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

        GlobalDeliveryPause savedPause = GlobalDeliveryPause.builder()
                .id(UUID.randomUUID())
                .customer(customer)
                .startDate(pauseStart)
                .endDate(pauseEnd)
                .build();
        when(globalDeliveryPauseRepository.save(any(GlobalDeliveryPause.class)))
                .thenReturn(savedPause);

        // Act
        GlobalDeliveryPauseResponse response =
                subscriptionService.addGlobalPause(customerId, request);

        // Assert — saved pause has the correct date range
        ArgumentCaptor<GlobalDeliveryPause> captor =
                ArgumentCaptor.forClass(GlobalDeliveryPause.class);
        verify(globalDeliveryPauseRepository).save(captor.capture());
        GlobalDeliveryPause captured = captor.getValue();
        assertThat(captured.getStartDate()).isEqualTo(pauseStart);
        assertThat(captured.getEndDate()).isEqualTo(pauseEnd);
        assertThat(captured.getCustomer().getId()).isEqualTo(customerId);
    }

    @Test
    @DisplayName("FR-SM8: Global pause with start date < 7 days away should be rejected")
    void addGlobalPause_shouldReject_whenStartDateIsLessThan7DaysAway() {
        // Arrange — trying to pause starting in only 3 days
        GlobalDeliveryPauseRequest request = new GlobalDeliveryPauseRequest();
        request.setStartDate(LocalDate.now().plusDays(3));
        request.setEndDate(LocalDate.now().plusDays(15));

        // Act & Assert
        assertThatThrownBy(() ->
                subscriptionService.addGlobalPause(customerId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("7 days advance notice");

        // Verify: nothing was saved to the database
        verify(globalDeliveryPauseRepository, never()).save(any());
    }

    @Test
    @DisplayName("FR-SM8: Global pause with end date before start date should be rejected")
    void addGlobalPause_shouldReject_whenEndDateIsBeforeStartDate() {
        // Arrange
        GlobalDeliveryPauseRequest request = new GlobalDeliveryPauseRequest();
        request.setStartDate(LocalDate.now().plusDays(10));
        request.setEndDate(LocalDate.now().plusDays(8)); // End is before start!

        // Act & Assert
        assertThatThrownBy(() ->
                subscriptionService.addGlobalPause(customerId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("end date cannot be before start date");

        verify(globalDeliveryPauseRepository, never()).save(any());
    }

    @Test
    @DisplayName("FR-SM7: cancelSubscription for non-existent subscription should throw RuntimeException")
    void cancelSubscription_shouldThrowException_whenSubscriptionNotFound() {
        // Arrange — subscription doesn't belong to this customer
        CancelSubscriptionRequest request = new CancelSubscriptionRequest();
        request.setCancelDate(LocalDate.now().plusDays(8));

        when(subscriptionRepository.findByIdAndCustomerId(subscriptionId, customerId))
                .thenReturn(Optional.empty()); // Not found!

        // Act & Assert — customer cannot access someone else's subscription
        assertThatThrownBy(() ->
                subscriptionService.cancelSubscription(customerId, subscriptionId, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Subscription not found");
    }
}
