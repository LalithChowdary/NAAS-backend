package com.naas.backend.billing.service;

import com.naas.backend.billing.dto.BillItemResponseDTO;
import com.naas.backend.billing.dto.BillResponseDTO;
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
import com.naas.backend.subscription.Subscription;
import com.naas.backend.subscription.SubscriptionRepository;
import com.naas.backend.subscription.SubscriptionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BillingService {

    private final BillRepository billRepository;
    private final CustomerRepository customerRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final DeliveryRecordRepository deliveryRecordRepository;
    private final PaymentRepository paymentRepository;

    // ── Monthly scheduled generation ────────────────────────────────
    @Scheduled(cron = "0 0 0 1 * ?")
    @Transactional
    public void scheduledGenerateMonthlyBills() {
        YearMonth previousMonth = YearMonth.now().minusMonths(1);
        generateBillsForMonth(previousMonth);
        checkAndDiscontinueOverdueSubscriptions();
    }

    @Transactional
    public void generateBillsForMonth(YearMonth yearMonth) {
        String billingMonth = yearMonth.format(DateTimeFormatter.ofPattern("MM-yyyy"));
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<Customer> activeCustomers = customerRepository.findAll().stream()
                .filter(Customer::isActive)
                .collect(Collectors.toList());

        for (Customer customer : activeCustomers) {
            if (billRepository.findByCustomerIdAndBillingMonth(customer.getId(), billingMonth).isPresent()) {
                continue;
            }

            List<Subscription> subscriptions = subscriptionRepository.findByCustomerId(customer.getId());
            if (subscriptions.isEmpty()) continue;

            List<BillItem> billItems = new ArrayList<>();
            BigDecimal totalAmount = BigDecimal.ZERO;

            for (Subscription sub : subscriptions) {
                long deliveriesCount = deliveryRecordRepository
                        .countByCustomerIdAndSubscriptionIdAndDeliveryDateBetweenAndStatus(
                                customer.getId(), sub.getId(), startDate, endDate,
                                DeliveryRecord.DeliveryStatus.DELIVERED);

                if (deliveriesCount > 0 && sub.getPublications() != null) {
                    for (com.naas.backend.publication.Publication pub : sub.getPublications()) {
                        BigDecimal pricePerUnit = BigDecimal.valueOf(pub.getPrice());
                        BigDecimal itemAmount = pricePerUnit.multiply(BigDecimal.valueOf(deliveriesCount));

                        BillItem item = BillItem.builder()
                                .publication(pub)
                                .deliveriesCount((int) deliveriesCount)
                                .pricePerUnit(pricePerUnit)
                                .itemAmount(itemAmount)
                                .build();

                        billItems.add(item);
                        totalAmount = totalAmount.add(itemAmount);
                    }
                }
            }

            if (!billItems.isEmpty()) {
                Bill bill = Bill.builder()
                        .customer(customer)
                        .billingMonth(billingMonth)
                        .totalAmount(totalAmount)
                        .dueDate(LocalDate.now().plusDays(15))
                        .items(new ArrayList<>())
                        .build();

                for (BillItem item : billItems) {
                    item.setBill(bill);
                    bill.getItems().add(item);
                }

                billRepository.save(bill);
            }
        }
    }

    // ── Fetch methods ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<BillResponseDTO> getAllBills() {
        return billRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BillResponseDTO> getCustomerBills(Long customerId) {
        return billRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BillResponseDTO getBillById(Long id) {
        Bill bill = billRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bill not found"));
        return mapToDTO(bill);
    }

    @Transactional(readOnly = true)
    public BillResponseDTO getCustomerBillById(Long customerId, Long billId) {
        Bill bill = billRepository.findById(billId)
                .filter(b -> b.getCustomer().getId().equals(customerId))
                .orElseThrow(() -> new RuntimeException("Bill not found or access denied"));
        return mapToDTO(bill);
    }

    // ── Overdue bills ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<BillResponseDTO> getOverdueBills() {
        LocalDate today = LocalDate.now();
        return billRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(b -> b.getStatus() == BillStatus.UNPAID && b.getDueDate().isBefore(today))
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // ── Payment recording ────────────────────────────────────────────

    @Transactional
    public PaymentResponseDTO recordPayment(Long billId, PaymentRequestDTO req) {
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new RuntimeException("Bill not found"));

        Payment.PaymentMethod method = Payment.PaymentMethod.valueOf(req.getPaymentMethod().toUpperCase());

        Payment payment = Payment.builder()
                .bill(bill)
                .customer(bill.getCustomer())
                .amount(req.getAmount())
                .paymentMethod(method)
                .chequeNumber(req.getChequeNumber())
                .receiptNote(req.getReceiptNote())
                .paidAt(LocalDateTime.now())
                .build();

        paymentRepository.save(payment);

        // Auto-mark bill as PAID if recorded amount >= totalAmount
        BigDecimal totalPaid = paymentRepository.findByBillIdOrderByPaidAtDesc(billId)
                .stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalPaid.compareTo(bill.getTotalAmount()) >= 0) {
            bill.setStatus(BillStatus.PAID);
            billRepository.save(bill);
        }

        return mapPaymentToDTO(payment);
    }

    @Transactional
    public BillResponseDTO markBillStatus(Long billId, String status) {
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new RuntimeException("Bill not found"));
        bill.setStatus(BillStatus.valueOf(status.toUpperCase()));
        billRepository.save(bill);
        return mapToDTO(bill);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponseDTO> getPaymentsForBill(Long billId) {
        return paymentRepository.findByBillIdOrderByPaidAtDesc(billId).stream()
                .map(this::mapPaymentToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PaymentResponseDTO> getCustomerPayments(Long customerId) {
        return paymentRepository.findByCustomerIdOrderByPaidAtDesc(customerId).stream()
                .map(this::mapPaymentToDTO)
                .collect(Collectors.toList());
    }

    // ── Overdue subscription discontinuation (FR-RDM3) ──────────────

    @Transactional
    public void checkAndDiscontinueOverdueSubscriptions() {
        // Discontinue subscriptions if dues unpaid for > 2 months
        LocalDate twoMonthsAgo = LocalDate.now().minusMonths(2);
        List<Bill> overdueBills = billRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(b -> b.getStatus() == BillStatus.UNPAID && b.getDueDate().isBefore(twoMonthsAgo))
                .collect(Collectors.toList());

        for (Bill bill : overdueBills) {
            List<Subscription> subs = subscriptionRepository.findByCustomerId(bill.getCustomer().getId());
            for (Subscription sub : subs) {
                if (sub.getStatus() == SubscriptionStatus.ACTIVE) {
                    sub.setStatus(SubscriptionStatus.CANCELLED);
                    sub.setEndDate(LocalDate.now());
                    subscriptionRepository.save(sub);
                }
            }
        }
    }

    // ── Mapping helpers ──────────────────────────────────────────────

    private BillResponseDTO mapToDTO(Bill bill) {
        List<BillItemResponseDTO> items = bill.getItems().stream()
                .map(item -> BillItemResponseDTO.builder()
                        .id(item.getId())
                        .publicationName(item.getPublication().getName())
                        .deliveriesCount(item.getDeliveriesCount())
                        .pricePerUnit(item.getPricePerUnit())
                        .itemAmount(item.getItemAmount())
                        .build())
                .collect(Collectors.toList());

        return BillResponseDTO.builder()
                .id(bill.getId())
                .customerId(bill.getCustomer().getId())
                .customerName(bill.getCustomer().getName())
                .customerAddress(bill.getCustomer().getAddress())
                .billingMonth(bill.getBillingMonth())
                .totalAmount(bill.getTotalAmount())
                .dueDate(bill.getDueDate())
                .status(bill.getStatus())
                .createdAt(bill.getCreatedAt())
                .items(items)
                .build();
    }

    private PaymentResponseDTO mapPaymentToDTO(Payment payment) {
        return PaymentResponseDTO.builder()
                .id(payment.getId())
                .billId(payment.getBill().getId())
                .billingMonth(payment.getBill().getBillingMonth())
                .customerId(payment.getCustomer().getId())
                .customerName(payment.getCustomer().getName())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod().name())
                .chequeNumber(payment.getChequeNumber())
                .receiptNote(payment.getReceiptNote())
                .paidAt(payment.getPaidAt())
                .build();
    }
}
