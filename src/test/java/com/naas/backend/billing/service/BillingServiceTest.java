package com.naas.backend.billing.service;

import com.naas.backend.billing.dto.PaymentRequestDTO;
import com.naas.backend.billing.dto.PaymentResponseDTO;
import com.naas.backend.billing.entity.Bill;
import com.naas.backend.billing.entity.BillItem;
import com.naas.backend.billing.entity.BillStatus;
import com.naas.backend.billing.entity.Payment;
import com.naas.backend.billing.repository.BillRepository;
import com.naas.backend.billing.repository.PaymentRepository;
import com.naas.backend.customer.Customer;
import com.naas.backend.customer.CustomerRepository;
import com.naas.backend.delivery.entity.DeliveryRecord;
import com.naas.backend.delivery.repository.DeliveryRecordRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for BillingService — Features 4, 6, 7, 8
 *
 * SRS References:
 *   FR-BM3:  The system shall calculate bill amounts based on active subscriptions
 *             and applicable business rules.
 *   FR-PAY4: The system shall track outstanding balances for each customer.
 *   FR-RDM2: The system shall generate reminder notices for unpaid dues after 1 month.
 *   FR-RDM3: The system shall discontinue subscriptions if dues remain unpaid
 *             for more than two months.
 */
@ExtendWith(MockitoExtension.class)
class BillingServiceTest {

    @Mock private BillRepository billRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private DeliveryRecordRepository deliveryRecordRepository;
    @Mock private PaymentRepository paymentRepository;

    @InjectMocks
    private BillingService billingService;

    // ── Shared test fixtures ─────────────────────────────────────────────────

    private Customer customer;
    private Publication publication1; // Price: ₹5.00/day
    private Publication publication2; // Price: ₹10.00/day
    private Subscription subscription;
    private UUID customerId;
    private UUID subscriptionId;
    private UUID billId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        subscriptionId = UUID.randomUUID();
        billId = UUID.randomUUID();

        customer = Customer.builder()
                .id(customerId)
                .name("Ravi Kumar")
                .active(true)
                .build();

        publication1 = new Publication();
        publication1.setId(UUID.randomUUID());
        publication1.setName("The Hindu");
        publication1.setPrice(5.0); // ₹5.00 per delivery

        publication2 = new Publication();
        publication2.setId(UUID.randomUUID());
        publication2.setName("Times of India");
        publication2.setPrice(10.0); // ₹10.00 per delivery

        SubscriptionItem item1 = SubscriptionItem.builder()
                .id(UUID.randomUUID())
                .publication(publication1)
                .status(SubscriptionItemStatus.ACTIVE)
                .build();

        SubscriptionItem item2 = SubscriptionItem.builder()
                .id(UUID.randomUUID())
                .publication(publication2)
                .status(SubscriptionItemStatus.ACTIVE)
                .build();

        subscription = Subscription.builder()
                .id(subscriptionId)
                .customer(customer)
                .status(SubscriptionStatus.ACTIVE)
                .startDate(LocalDate.now().minusMonths(2))
                .items(new ArrayList<>(List.of(item1, item2)))
                .build();
    }

    // ── FR-BM3: Bill Calculation Engine ──────────────────────────────────────

    @Test
    @DisplayName("FR-BM3: Monthly bill should be (deliveries × price1) + (deliveries × price2)")
    void generateBillsForMonth_shouldCalculateTotalCorrectly_basedOnDeliveredCount() {
        // Arrange — simulate a month where 20 deliveries were made
        // Expected bill = 20 days × ₹5 + 20 days × ₹10 = ₹100 + ₹200 = ₹300
        YearMonth testMonth = YearMonth.now().minusMonths(1);
        long deliveredDays = 20L;

        when(customerRepository.findAll()).thenReturn(List.of(customer));
        when(billRepository.findByCustomerIdAndBillingMonth(eq(customerId), any()))
                .thenReturn(Optional.empty()); // No existing bill for this month
        when(subscriptionRepository.findByCustomerId(customerId))
                .thenReturn(List.of(subscription));

        // Mock that the delivery repository confirms 20 DELIVERED records for the subscription
        when(deliveryRecordRepository.countByCustomerIdAndSubscriptionIdAndDeliveryDateBetweenAndStatus(
                eq(customerId), eq(subscriptionId), any(LocalDate.class), any(LocalDate.class),
                eq(DeliveryRecord.DeliveryStatus.DELIVERED)))
                .thenReturn(deliveredDays);

        // Act
        billingService.generateBillsForMonth(testMonth);

        // Assert — a Bill was saved; capture it and validate the total amount
        ArgumentCaptor<Bill> billCaptor = ArgumentCaptor.forClass(Bill.class);
        verify(billRepository).save(billCaptor.capture());
        Bill savedBill = billCaptor.getValue();

        // pub1: 20 × 5 = 100, pub2: 20 × 10 = 200, Total = ₹300
        BigDecimal expectedTotal = new BigDecimal("300.0");
        assertThat(savedBill.getTotalAmount()).isEqualByComparingTo(expectedTotal);
        assertThat(savedBill.getItems()).hasSize(2); // One BillItem per each publication
    }

    @Test
    @DisplayName("FR-BM3: No deliveries in a month should NOT generate a bill for the customer")
    void generateBillsForMonth_shouldNotCreateBill_whenZeroDeliveriesExist() {
        // Arrange — simulate a month with ZERO deliveries (customer was on pause)
        YearMonth testMonth = YearMonth.now().minusMonths(1);

        when(customerRepository.findAll()).thenReturn(List.of(customer));
        when(billRepository.findByCustomerIdAndBillingMonth(any(), any()))
                .thenReturn(Optional.empty());
        when(subscriptionRepository.findByCustomerId(customerId))
                .thenReturn(List.of(subscription));

        // Mock: 0 deliveries for this subscription
        when(deliveryRecordRepository.countByCustomerIdAndSubscriptionIdAndDeliveryDateBetweenAndStatus(
                any(), any(), any(), any(), any()))
                .thenReturn(0L);

        // Act
        billingService.generateBillsForMonth(testMonth);

        // Assert — billing system must NOT create a bill for zero deliveries
        verify(billRepository, never()).save(any(Bill.class));
    }

    @Test
    @DisplayName("FR-BM3: Bill should not be regenerated if one already exists for the month")
    void generateBillsForMonth_shouldSkip_whenBillAlreadyExistsForMonth() {
        // Arrange — a bill was already generated this month (idempotency check)
        YearMonth testMonth = YearMonth.now().minusMonths(1);
        Bill existingBill = Bill.builder()
                .id(billId)
                .customer(customer)
                .totalAmount(new BigDecimal("300.0"))
                .build();

        when(customerRepository.findAll()).thenReturn(List.of(customer));
        when(billRepository.findByCustomerIdAndBillingMonth(eq(customerId), any()))
                .thenReturn(Optional.of(existingBill)); // Bill already exists!

        // Act
        billingService.generateBillsForMonth(testMonth);

        // Assert — no new bill should be saved; delivery records not even queried
        verify(billRepository, never()).save(any());
        verify(deliveryRecordRepository, never())
                .countByCustomerIdAndSubscriptionIdAndDeliveryDateBetweenAndStatus(
                        any(), any(), any(), any(), any());
    }

    // ── FR-PAY4: Outstanding Balance Tracking ────────────────────────────────

    @Test
    @DisplayName("FR-PAY4: Partial payment should NOT mark bill as PAID")
    void recordPayment_shouldNotMarkBillAsPaid_whenPaymentIsPartial() {
        // Arrange — Bill total is ₹500, customer pays only ₹300 (partial)
        Bill bill = Bill.builder()
                .id(billId)
                .customer(customer)
                .totalAmount(new BigDecimal("500.00"))
                .dueDate(LocalDate.now().plusDays(15))
                .status(BillStatus.UNPAID)
                .items(new ArrayList<>())
                .build();

        PaymentRequestDTO req = new PaymentRequestDTO();
        req.setAmount(new BigDecimal("300.00")); // Partial payment
        req.setPaymentMethod("CASH");

        when(billRepository.findById(billId)).thenReturn(Optional.of(bill));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        // Total paid so far = ₹300 (less than ₹500)
        when(paymentRepository.findByBillIdOrderByPaidAtDesc(billId))
                .thenReturn(List.of(Payment.builder()
                        .amount(new BigDecimal("300.00"))
                        .build()));

        // Act
        billingService.recordPayment(billId, req);

        // Assert — bill should remain UNPAID (balance outstanding = ₹200)
        assertThat(bill.getStatus()).isEqualTo(BillStatus.UNPAID);
        // Verify the bill was NOT re-saved with a PAID status
        verify(billRepository, never()).save(any(Bill.class));
    }

    @Test
    @DisplayName("FR-PAY4: Full payment should auto-mark bill status as PAID")
    void recordPayment_shouldMarkBillAsPaid_whenFullAmountIsRecorded() {
        // Arrange — Bill total is ₹500, customer pays ₹500 in full
        Bill bill = Bill.builder()
                .id(billId)
                .customer(customer)
                .totalAmount(new BigDecimal("500.00"))
                .dueDate(LocalDate.now().plusDays(15))
                .status(BillStatus.UNPAID)
                .items(new ArrayList<>())
                .build();

        PaymentRequestDTO req = new PaymentRequestDTO();
        req.setAmount(new BigDecimal("500.00")); // Full payment
        req.setPaymentMethod("CASH");

        when(billRepository.findById(billId)).thenReturn(Optional.of(bill));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        // Total paid so far = ₹500 (equals bill total)
        when(paymentRepository.findByBillIdOrderByPaidAtDesc(billId))
                .thenReturn(List.of(Payment.builder()
                        .amount(new BigDecimal("500.00"))
                        .build()));
        when(billRepository.save(any(Bill.class))).thenReturn(bill);

        // Act
        billingService.recordPayment(billId, req);

        // Assert — bill must now be marked as PAID
        assertThat(bill.getStatus()).isEqualTo(BillStatus.PAID);
        verify(billRepository).save(bill);
    }

    @Test
    @DisplayName("FR-PAY4: Two partial payments totalling full amount should mark bill as PAID")
    void recordPayment_shouldMarkBillAsPaid_whenCumulativePaymentsMeetTotal() {
        // Arrange — Previously paid ₹300, now paying remaining ₹200 (total = ₹500)
        Bill bill = Bill.builder()
                .id(billId)
                .customer(customer)
                .totalAmount(new BigDecimal("500.00"))
                .dueDate(LocalDate.now().plusDays(15))
                .status(BillStatus.UNPAID)
                .items(new ArrayList<>())
                .build();

        PaymentRequestDTO req = new PaymentRequestDTO();
        req.setAmount(new BigDecimal("200.00")); // Second payment
        req.setPaymentMethod("CHEQUE");
        req.setChequeNumber("CHQ-12345");

        when(billRepository.findById(billId)).thenReturn(Optional.of(bill));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        // Two payments combined: ₹300 + ₹200 = ₹500 (equals total)
        when(paymentRepository.findByBillIdOrderByPaidAtDesc(billId))
                .thenReturn(List.of(
                        Payment.builder().amount(new BigDecimal("300.00")).build(),
                        Payment.builder().amount(new BigDecimal("200.00")).build()
                ));
        when(billRepository.save(any(Bill.class))).thenReturn(bill);

        // Act
        billingService.recordPayment(billId, req);

        // Assert — cumulative payments hit ₹500, so bill should be PAID
        assertThat(bill.getStatus()).isEqualTo(BillStatus.PAID);
    }

    // ── FR-RDM3: Subscription Discontinuation for > 2 Months Overdue ─────────

    @Test
    @DisplayName("FR-RDM3: Active subscription should be CANCELLED if bill is overdue > 2 months")
    void checkAndDiscontinueOverdueSubscriptions_shouldCancelActiveSubscription_whenDuesExceedTwoMonths() {
        // Arrange — bill was due more than 2 months ago and is still UNPAID
        LocalDate overdueDate = LocalDate.now().minusMonths(3); // 3 months old = > 2 month threshold
        Bill overdueBill = Bill.builder()
                .id(billId)
                .customer(customer)
                .totalAmount(new BigDecimal("300.00"))
                .dueDate(overdueDate)
                .status(BillStatus.UNPAID) // Still not paid!
                .items(new ArrayList<>())
                .build();

        when(billRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(overdueBill));
        when(subscriptionRepository.findByCustomerId(customerId))
                .thenReturn(List.of(subscription)); // Customer has an active subscription
        when(subscriptionRepository.save(any(Subscription.class)))
                .thenReturn(subscription);

        // Act
        billingService.checkAndDiscontinueOverdueSubscriptions();

        // Assert — the subscription must have been marked as CANCELLED
        ArgumentCaptor<Subscription> subCaptor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository).save(subCaptor.capture());
        assertThat(subCaptor.getValue().getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
        assertThat(subCaptor.getValue().getEndDate()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("FR-RDM3: Already cancelled subscription should NOT be saved again")
    void checkAndDiscontinueOverdueSubscriptions_shouldSkip_forAlreadyCancelledSubscriptions() {
        // Arrange — overdue bill but this subscription is already CANCELLED
        LocalDate overdueDate = LocalDate.now().minusMonths(3);
        Bill overdueBill = Bill.builder()
                .id(billId)
                .customer(customer)
                .totalAmount(new BigDecimal("300.00"))
                .dueDate(overdueDate)
                .status(BillStatus.UNPAID)
                .items(new ArrayList<>())
                .build();

        Subscription cancelledSub = Subscription.builder()
                .id(subscriptionId)
                .customer(customer)
                .status(SubscriptionStatus.CANCELLED) // Already cancelled
                .startDate(LocalDate.now().minusMonths(3))
                .build();

        when(billRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(overdueBill));
        when(subscriptionRepository.findByCustomerId(customerId))
                .thenReturn(List.of(cancelledSub));

        // Act
        billingService.checkAndDiscontinueOverdueSubscriptions();

        // Assert — no redundant save should occur for an already-cancelled subscription
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("FR-RDM2: Paid bill should NOT trigger overdue discontinuation logic")
    void checkAndDiscontinueOverdueSubscriptions_shouldNotAffect_customerWithPaidBills() {
        // Arrange — a bill that looks old but is actually PAID
        LocalDate oldDate = LocalDate.now().minusMonths(3);
        Bill paidBill = Bill.builder()
                .id(billId)
                .customer(customer)
                .totalAmount(new BigDecimal("300.00"))
                .dueDate(oldDate)
                .status(BillStatus.PAID) // Already paid — should not trigger discontinuation
                .items(new ArrayList<>())
                .build();

        when(billRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(paidBill));

        // Act
        billingService.checkAndDiscontinueOverdueSubscriptions();

        // Assert — subscription lookups should not even happen for paid bills
        verify(subscriptionRepository, never()).findByCustomerId(any());
        verify(subscriptionRepository, never()).save(any());
    }
}
