package com.naas.backend.delivery.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Automatically generates daily delivery schedules at 12:01 AM every day.
 * Uses FleetRoutingService for GPS-optimized assignment and round-robin fallback.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryScheduler {

    private final DeliveryService deliveryService;

    /**
     * Fires at 12:01 AM every day.
     * Cron: second=0, minute=1, hour=0, every day, every month, every day-of-week.
     */
    @Scheduled(cron = "0 1 0 * * *")
    public void generateDailySchedule() {
        LocalDate today = LocalDate.now();
        log.info("[DeliveryScheduler] ⏰ Triggering automatic delivery schedule generation for {}", today);
        try {
            deliveryService.generateSchedulesForDate(today);
            log.info("[DeliveryScheduler] ✅ Delivery schedules successfully generated for {}", today);
        } catch (Exception e) {
            log.error("[DeliveryScheduler] ❌ Failed to generate delivery schedules for {}: {}", today, e.getMessage(), e);
        }
    }
}
