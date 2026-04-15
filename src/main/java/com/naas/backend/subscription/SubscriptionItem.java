package com.naas.backend.subscription;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.naas.backend.publication.Publication;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "subscription_items")
public class SubscriptionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private Subscription subscription;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "publication_id", nullable = false)
    private Publication publication;

    @Column(nullable = false)
    @Builder.Default
    private String frequency = "DAILY";

    @Column(name = "custom_delivery_days")
    private String customDeliveryDays;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SubscriptionItemStatus status = SubscriptionItemStatus.ACTIVE;

    @Column(name = "stop_start_date")
    private LocalDate stopStartDate;

    @Column(name = "stop_end_date")
    private LocalDate stopEndDate;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null)
            createdAt = LocalDateTime.now();
    }

    public boolean isActiveOn(LocalDate date) {
        if (this.getStatus() == SubscriptionItemStatus.REMOVED) {
            return false;
        }
        if (this.getStatus() == SubscriptionItemStatus.SUSPENDED) {
            if (this.getStopStartDate() != null && this.getStopEndDate() != null) {
                if ((date.isEqual(this.getStopStartDate()) || date.isAfter(this.getStopStartDate())) &&
                        (date.isEqual(this.getStopEndDate()) || date.isBefore(this.getStopEndDate()))) {
                    return false;
                }
            }
        }

        // Frequency Check
        String freq = this.getFrequency() != null ? this.getFrequency().toUpperCase() : "DAILY";

        switch (freq) {
            case "DAILY":
                return true;
            case "WEEKLY":
                // e.g. Deliver on Sunday if WEEKLY
                return date.getDayOfWeek() == java.time.DayOfWeek.SUNDAY;
            case "MONTHLY":
                // e.g. Deliver on the 1st of the month if MONTHLY
                return date.getDayOfMonth() == 1;
            case "ALTERNATE":
                // Simple rule: deliver on even days of counting epoch, or just basic day of
                // year check
                return date.getDayOfYear() % 2 == 0;
            case "CUSTOM":
                String customDays = this.getCustomDeliveryDays();
                if (customDays != null && !customDays.isEmpty()) {
                    String todayName = date.getDayOfWeek().name();
                    return customDays.toUpperCase().contains(todayName);
                }
                return false;
            default:
                return true; // Fallback to daily
        }
    }
}
