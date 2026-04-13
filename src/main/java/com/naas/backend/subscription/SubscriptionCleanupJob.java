package com.naas.backend.subscription;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class SubscriptionCleanupJob {

    private final SubscriptionRepository subscriptionRepository;

    @Scheduled(cron = "0 0 0 * * ?") // Runs every day at midnight (midnight relative to server time)
    @Transactional
    public void removeExpiredSuspensions() {
        LocalDate today = LocalDate.now();
        log.info("Running daily cleanup to remove expired subscriptions limits for date {}", today);

        // Fetch all subscriptions
        List<Subscription> subscriptions = subscriptionRepository.findAll();
        for (Subscription sub : subscriptions) {
            boolean updated = false;

            // Clear subscription-level suspensions that have ended
            if (sub.getSuspendEndDate() != null && sub.getSuspendEndDate().isBefore(today)) {
                sub.setSuspendStartDate(null);
                sub.setSuspendEndDate(null);
                updated = true;
                log.info("Removed expired suspension on Subscription {}", sub.getId());
            }

            // Clear item-level suspensions that have ended
            if (sub.getItems() != null) {
                for (SubscriptionItem item : sub.getItems()) {
                    if (item.getStopEndDate() != null && item.getStopEndDate().isBefore(today)) {
                        item.setStopStartDate(null);
                        item.setStopEndDate(null);
                        if (item.getStatus() == SubscriptionItemStatus.SUSPENDED) {
                            item.setStatus(SubscriptionItemStatus.ACTIVE);
                        }
                        updated = true;
                        log.info("Removed expired suspension on Item {}", item.getId());
                    }
                }
            }

            // Save modifications back to DB
            if (updated) {
                subscriptionRepository.save(sub);
            }
        }
    }
}
