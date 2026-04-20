package com.naas.backend.deliveryperson.service;

import com.naas.backend.deliveryperson.DeliveryPerson;
import com.naas.backend.deliveryperson.DeliveryPersonPayout;
import com.naas.backend.deliveryperson.DeliveryPersonPayoutRepository;
import com.naas.backend.deliveryperson.DeliveryPersonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

/**
 * Automatically calculates and records the monthly payout for every approved
 * delivery person at 23:57 on the last day of each month.
 *
 * Payout = 2.5% of the total value of all DELIVERED subscriptions in the month.
 * A DeliveryPersonPayout record is created with status PENDING, ready for
 * the admin to confirm payment via the existing /api/admin/delivery-persons/{id}/payout endpoint.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryPersonPayoutScheduler {

    private final DeliveryPersonRepository deliveryPersonRepository;
    private final DeliveryPersonPayoutRepository deliveryPersonPayoutRepository;
    private final DeliveryPersonService deliveryPersonService;

    /**
     * Fires at 23:57 on the last day of every month — two minutes before reminder
     * processing so payouts are computed before any subscription cancellations occur.
     */
    @Scheduled(cron = "0 57 23 L * *")
    @Transactional
    public void generateMonthlyPayouts() {
        YearMonth thisMonth = YearMonth.now();
        LocalDate startDate = thisMonth.atDay(1);
        LocalDate endDate   = thisMonth.atEndOfMonth();

        log.info("[PayoutScheduler] ⏰ Generating monthly payouts for {} ({} → {})",
                thisMonth, startDate, endDate);

        // Use JOIN FETCH query so User is eagerly loaded — avoids LazyInitializationException
        // and correctly excludes disabled accounts (user.active=false).
        List<DeliveryPerson> approved = deliveryPersonRepository.findAllApprovedAndActive();

        int generated = 0;
        int skipped   = 0;

        for (DeliveryPerson person : approved) {

            // Skip if a payout record already exists for this person and period
            boolean alreadyExists = !deliveryPersonPayoutRepository
                    .findByDeliveryPersonIdAndStartDateAndEndDate(person.getId(), startDate, endDate)
                    .isEmpty();

            if (alreadyExists) {
                log.debug("[PayoutScheduler] Payout already exists for {} ({}) – skipping.",
                        person.getName(), thisMonth);
                skipped++;
                continue;
            }

            // Calculate earned amount using existing 2.5% commission logic
            Double earned = deliveryPersonService.calculatePayout(person.getId(), startDate, endDate);

            if (earned == null || earned <= 0.0) {
                log.info("[PayoutScheduler] No deliveries found for {} in {} – no payout created.",
                        person.getName(), thisMonth);
                skipped++;
                continue;
            }

            DeliveryPersonPayout payout = DeliveryPersonPayout.builder()
                    .deliveryPerson(person)
                    .startDate(startDate)
                    .endDate(endDate)
                    .amountPaid(BigDecimal.valueOf(earned))
                    .paymentDate(LocalDateTime.now())
                    .status(DeliveryPersonPayout.PayoutStatus.PENDING)
                    .build();

            deliveryPersonPayoutRepository.save(payout);

            log.info("[PayoutScheduler] ✅ Payout CREATED | Person: {} ({}) | Month: {} | Amount: ₹{} | Status: PENDING",
                    person.getName(), person.getEmployeeId(), thisMonth,
                    String.format("%.2f", earned));
            generated++;
        }

        log.info("[PayoutScheduler] 🏁 Payout generation complete for {} | Created: {} | Skipped/Zero: {}",
                thisMonth, generated, skipped);
    }
}
