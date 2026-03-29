package com.naas.backend.billing.service;

import com.naas.backend.billing.dto.BillItemResponseDTO;
import com.naas.backend.billing.dto.BillResponseDTO;
import com.naas.backend.billing.entity.Bill;
import com.naas.backend.billing.entity.BillItem;
import com.naas.backend.billing.repository.BillRepository;
import com.naas.backend.customer.Customer;
import com.naas.backend.customer.CustomerRepository;
import com.naas.backend.delivery.entity.DeliveryRecord;
import com.naas.backend.delivery.repository.DeliveryRecordRepository;
import com.naas.backend.publication.PublicationRepository;
import com.naas.backend.subscription.Subscription;
import com.naas.backend.subscription.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    private final PublicationRepository publicationRepository;

    // Cron expression: At 00:00:00 on the 1st day of every month
    @Scheduled(cron = "0 0 0 1 * ?")
    @Transactional
    public void scheduledGenerateMonthlyBills() {
        // Generate for previous month
        YearMonth previousMonth = YearMonth.now().minusMonths(1);
        generateBillsForMonth(previousMonth);
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
            // Check if already billed
            if (billRepository.findByCustomerIdAndBillingMonth(customer.getId(), billingMonth).isPresent()) {
                continue;
            }

            List<Subscription> subscriptions = subscriptionRepository.findByCustomerId(customer.getId());
            if (subscriptions.isEmpty())
                continue;

            List<BillItem> billItems = new ArrayList<>();
            BigDecimal totalAmount = BigDecimal.ZERO;

            for (Subscription sub : subscriptions) {
                long deliveriesCount = deliveryRecordRepository
                        .countByCustomerIdAndSubscriptionIdAndDeliveryDateBetweenAndStatus(
                                customer.getId(),
                                sub.getId(),
                                startDate,
                                endDate,
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
                        .dueDate(LocalDate.now().plusDays(15)) // Due in 15 days
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
}
