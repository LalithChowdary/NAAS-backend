package com.naas.backend.report;

import com.naas.backend.report.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import com.naas.backend.delivery.repository.DeliveryRecordRepository;
import com.naas.backend.subscription.SubscriptionRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ReportService — Reporting & Summary Generation.
 *
 * SRS Coverage:
 * FR-RPT1: Monthly summary report (subscriptions, billing, deliveries).
 * FR-RPT2: Outstanding dues report (unpaid bills per customer).
 * FR-RPT3: Delivery performance summary report.
 * FR-RPT4: Delivery personnel commission/payment report.
 * FR-RPT5: "Who received what" delivery log report.
 *
 * Design Note:
 * ReportService uses JdbcTemplate directly (raw SQL) instead of
 * JPA repositories. Therefore, we mock JdbcTemplate itself.
 * Two types of JdbcTemplate calls are mocked:
 * - queryForObject() → for scalar aggregate values (COUNT, SUM)
 * - query() → for result-set lists mapped via RowMapper
 *
 * Run command:
 * ./mvnw test -Dgroups=!e2e (runs all unit tests, skips E2E)
 */
@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

        @Mock
        private JdbcTemplate jdbcTemplate;

        @Mock
        private DeliveryRecordRepository deliveryRecordRepository;

        @Mock
        private SubscriptionRepository subscriptionRepository;

        @InjectMocks
        private ReportService reportService;

        // ─────────────────────────────────────────────────────────────────────────
        // FR-RPT1: Monthly Summary Report
        // ─────────────────────────────────────────────────────────────────────────

        @Test
        @DisplayName("FR-RPT1-1: getMonthlySummary should aggregate active subs, billing, payment, and delivery counts correctly")
        void getMonthlySummary_shouldReturnCorrectAggregates() {
                // Arrange — stub the 5 queryForObject scalar calls in order of execution
                when(jdbcTemplate.queryForObject(contains("COUNT(*) FROM subscriptions"), eq(Long.class)))
                                .thenReturn(42L);

                when(jdbcTemplate.queryForObject(contains("SUM(total_amount) FROM bills"), eq(BigDecimal.class),
                                anyString()))
                                .thenReturn(new BigDecimal("15000.00"));

                when(jdbcTemplate.queryForObject(contains("SUM(amount) FROM payments"), eq(BigDecimal.class), any(),
                                any()))
                                .thenReturn(new BigDecimal("12000.00"));

                when(jdbcTemplate.queryForObject(contains("COUNT(*) FROM delivery_records WHERE delivery_date"),
                                eq(Long.class), any(LocalDate.class), any(LocalDate.class)))
                                .thenReturn(310L) // first call: totalDeliveries
                                .thenReturn(290L); // second call: successfulDeliveries

                // Act
                MonthlySummaryResponse result = reportService.getMonthlySummary("04-2026");

                // Assert
                assertThat(result.getMonth()).isEqualTo("04-2026");
                assertThat(result.getTotalActiveSubscriptions()).isEqualTo(42L);
                assertThat(result.getTotalBilled()).isEqualByComparingTo("15000.00");
                assertThat(result.getTotalCollected()).isEqualByComparingTo("12000.00");
                assertThat(result.getTotalDeliveries()).isEqualTo(310L);
                assertThat(result.getSuccessfulDeliveries()).isEqualTo(290L);
        }

        @Test
        @DisplayName("FR-RPT1-2: getMonthlySummary should default to ZERO for months with no billing data (null from DB)")
        void getMonthlySummary_whenNoBillingData_shouldReturnZeroDefaults() {
                // Arrange — DB returns null for all aggregate queries (no data for this month)
                when(jdbcTemplate.queryForObject(contains("COUNT(*) FROM subscriptions"), eq(Long.class)))
                                .thenReturn(0L);
                when(jdbcTemplate.queryForObject(contains("SUM(total_amount) FROM bills"), eq(BigDecimal.class),
                                anyString()))
                                .thenReturn(null); // SQL SUM returns NULL when no rows match
                when(jdbcTemplate.queryForObject(contains("SUM(amount) FROM payments"), eq(BigDecimal.class), any(),
                                any()))
                                .thenReturn(null);
                when(jdbcTemplate.queryForObject(contains("COUNT(*) FROM delivery_records WHERE delivery_date"),
                                eq(Long.class), any(LocalDate.class), any(LocalDate.class)))
                                .thenReturn(null)
                                .thenReturn(null);

                // Act
                MonthlySummaryResponse result = reportService.getMonthlySummary("01-2023");

                // Assert — nulls must be mapped to safe zero values (not throw
                // NullPointerException)
                assertThat(result.getTotalBilled()).isEqualByComparingTo(BigDecimal.ZERO);
                assertThat(result.getTotalCollected()).isEqualByComparingTo(BigDecimal.ZERO);
                assertThat(result.getTotalDeliveries()).isEqualTo(0L);
                assertThat(result.getSuccessfulDeliveries()).isEqualTo(0L);
        }

        @Test
        @DisplayName("FR-RPT1-3: getMonthlySummary should correctly parse 'MM-YYYY' format to calculate date range")
        void getMonthlySummary_shouldParseMonthStringToCalculateDateRange() {
                // Arrange
                when(jdbcTemplate.queryForObject(contains("COUNT(*) FROM subscriptions"), eq(Long.class)))
                                .thenReturn(5L);
                when(jdbcTemplate.queryForObject(contains("SUM(total_amount)"), eq(BigDecimal.class), anyString()))
                                .thenReturn(null);
                when(jdbcTemplate.queryForObject(contains("SUM(amount)"), eq(BigDecimal.class), any(), any()))
                                .thenReturn(null);
                when(jdbcTemplate.queryForObject(contains("COUNT(*) FROM delivery_records"), eq(Long.class),
                                any(LocalDate.class), any(LocalDate.class)))
                                .thenReturn(0L).thenReturn(0L);

                // Act — February 2024 (leap year, should have 29 days)
                MonthlySummaryResponse result = reportService.getMonthlySummary("02-2024");

                // Assert — no exception, month preserved in response
                assertThat(result.getMonth()).isEqualTo("02-2024");
        }

        // ─────────────────────────────────────────────────────────────────────────
        // FR-RPT2: Outstanding Dues Report
        // ─────────────────────────────────────────────────────────────────────────

        @Test
        @DisplayName("FR-RPT2-1: getOutstandingDues should return all customers with unpaid bills")
        void getOutstandingDues_shouldReturnListOfDefaulters() {
                // Arrange — mock a list of 2 customers with outstanding dues
                List<OutstandingDuesResponse> mockDues = List.of(
                                OutstandingDuesResponse.builder()
                                                .customerName("Alice Kumar").email("alice@test.com")
                                                .phoneNumber("9999999999").totalDue(new BigDecimal("1200.00"))
                                                .pendingBillsCount(3).build(),
                                OutstandingDuesResponse.builder()
                                                .customerName("Bob Singh").email("bob@test.com")
                                                .phoneNumber("8888888888").totalDue(new BigDecimal("450.00"))
                                                .pendingBillsCount(1).build());

                when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<OutstandingDuesResponse>>any()))
                                .thenReturn(mockDues);

                // Act
                List<OutstandingDuesResponse> result = reportService.getOutstandingDues();

                // Assert
                assertThat(result).hasSize(2);
                assertThat(result.get(0).getCustomerName()).isEqualTo("Alice Kumar");
                assertThat(result.get(0).getTotalDue()).isEqualByComparingTo("1200.00");
                assertThat(result.get(0).getPendingBillsCount()).isEqualTo(3);
                // Result should be ordered by totalDue DESC (highest first)
                assertThat(result.get(0).getTotalDue()).isGreaterThan(result.get(1).getTotalDue());
        }

        @Test
        @DisplayName("FR-RPT2-2: getOutstandingDues should return empty list when all customers are fully paid")
        void getOutstandingDues_whenNoPendingBills_shouldReturnEmptyList() {
                // Arrange
                when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<OutstandingDuesResponse>>any()))
                                .thenReturn(List.of());

                // Act
                List<OutstandingDuesResponse> result = reportService.getOutstandingDues();

                // Assert
                assertThat(result).isEmpty();
        }

        // ─────────────────────────────────────────────────────────────────────────
        // FR-RPT3: Delivery Performance Summary
        // ─────────────────────────────────────────────────────────────────────────

        @Test
        @DisplayName("FR-RPT3-1: getDeliverySummary should return per-day delivery breakdown within date range")
        void getDeliverySummary_shouldReturnDailyBreakdown() {
                // Arrange
                LocalDate start = LocalDate.of(2026, 4, 1);
                LocalDate end = LocalDate.of(2026, 4, 3);

                List<DeliverySummaryResponse> mockSummary = List.of(
                                DeliverySummaryResponse.builder().date(start).totalAssigned(100L).delivered(90L)
                                                .cancelled(5L).pending(5L).build(),
                                DeliverySummaryResponse.builder().date(start.plusDays(1)).totalAssigned(95L)
                                                .delivered(85L).cancelled(3L).pending(7L).build(),
                                DeliverySummaryResponse.builder().date(start.plusDays(2)).totalAssigned(105L)
                                                .delivered(100L).cancelled(2L).pending(3L).build());

                when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<DeliverySummaryResponse>>any(),
                                any(LocalDate.class), any(LocalDate.class)))
                                .thenReturn(mockSummary);

                // Act
                List<DeliverySummaryResponse> result = reportService.getDeliverySummary(start, end);

                // Assert
                assertThat(result).hasSize(3);
                assertThat(result.get(0).getDate()).isEqualTo(LocalDate.of(2026, 4, 1));
                assertThat(result.get(0).getTotalAssigned()).isEqualTo(100L);
                assertThat(result.get(0).getDelivered()).isEqualTo(90L);
                assertThat(result.get(0).getCancelled()).isEqualTo(5L);
                assertThat(result.get(0).getPending()).isEqualTo(5L);
        }

        @Test
        @DisplayName("FR-RPT3-2: getDeliverySummary for a date range with no deliveries should return empty list")
        void getDeliverySummary_whenNoDeliveries_shouldReturnEmptyList() {
                // Arrange
                when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<DeliverySummaryResponse>>any(),
                                any(LocalDate.class), any(LocalDate.class)))
                                .thenReturn(List.of());

                // Act
                List<DeliverySummaryResponse> result = reportService.getDeliverySummary(
                                LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 31));

                // Assert
                assertThat(result).isEmpty();
        }

        // ─────────────────────────────────────────────────────────────────────────
        // FR-RPT4: Delivery Personnel Commission/Payment Report
        // ─────────────────────────────────────────────────────────────────────────

        @Test
        @DisplayName("FR-RPT4-1: getDeliveryPersonnelPayment should calculate 2.5% commission per person")
        void getDeliveryPersonnelPayment_shouldCalculateCommissionCorrectly() {
                // Arrange — DB returns raw delivery value; service applies 2.5% commission
                UUID dpId = UUID.randomUUID();
                UUID subId = UUID.randomUUID();
                List<DeliveryPersonnelPaymentResponse> mockData = List.of(
                                DeliveryPersonnelPaymentResponse.builder()
                                                .deliveryPersonId(dpId)
                                                .deliveryPersonName("Raju Delivery")
                                                .employeeId("EMP001")
                                                .deliveriesCompleted(80L)
                                                .alreadyPaid(BigDecimal.ZERO)
                                                .build());

                when(jdbcTemplate.query(anyString(),
                                ArgumentMatchers.<RowMapper<DeliveryPersonnelPaymentResponse>>any(), any(), any(),
                                any(), any(), any(), any(), any(), any()))
                                .thenReturn(mockData);

                com.naas.backend.delivery.entity.DeliveryRecord rec = com.naas.backend.delivery.entity.DeliveryRecord
                                .builder()
                                .subscriptionId(subId).deliveryDate(LocalDate.of(2026, 4, 15)).build();
                when(deliveryRecordRepository.findByDeliveryPersonIdAndDeliveryDateBetweenAndStatus(
                                eq(dpId), any(), any(), any())).thenReturn(List.of(rec));

                com.naas.backend.publication.Publication pub = new com.naas.backend.publication.Publication();
                pub.setPrice(10000.00);

                com.naas.backend.subscription.Subscription sub = com.naas.backend.subscription.Subscription.builder()
                                .id(subId)
                                .items(List.of(
                                                com.naas.backend.subscription.SubscriptionItem.builder()
                                                                .status(com.naas.backend.subscription.SubscriptionItemStatus.ACTIVE)
                                                                .frequency("DAILY")
                                                                .publication(pub)
                                                                .build()))
                                .build();
                when(subscriptionRepository.findById(subId)).thenReturn(java.util.Optional.of(sub));

                // Act
                List<DeliveryPersonnelPaymentResponse> result = reportService.getDeliveryPersonnelPayment(
                                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30));

                // Assert
                assertThat(result).hasSize(1);
                assertThat(result.get(0).getDeliveryPersonName()).isEqualTo("Raju Delivery");
                assertThat(result.get(0).getTotalDeliveryValue()).isEqualByComparingTo("10000.00");
                assertThat(result.get(0).getPaymentAmount()).isEqualByComparingTo("250.00"); // exactly 2.5%
                assertThat(result.get(0).getDeliveriesCompleted()).isEqualTo(80L);
        }

        @Test
        @DisplayName("FR-RPT4-2: getDeliveryPersonnelPayment with no deliveries should return empty list")
        void getDeliveryPersonnelPayment_whenNoDeliveries_shouldReturnEmptyList() {
                // Arrange
                when(jdbcTemplate.query(anyString(),
                                ArgumentMatchers.<RowMapper<DeliveryPersonnelPaymentResponse>>any(), any(), any(),
                                any(), any(), any(), any(), any(), any()))
                                .thenReturn(List.of());

                // Act
                List<DeliveryPersonnelPaymentResponse> result = reportService.getDeliveryPersonnelPayment(
                                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30));

                // Assert
                assertThat(result).isEmpty();
        }

        // ─────────────────────────────────────────────────────────────────────────
        // FR-RPT5: "Who Received What" Delivery Log Report
        // ─────────────────────────────────────────────────────────────────────────

        @Test
        @DisplayName("FR-RPT5-1: getWhoReceivedWhat should return per-delivery log with customer, address, and publication")
        void getWhoReceivedWhat_shouldReturnDetailedDeliveryLog() {
                // Arrange
                LocalDate date = LocalDate.of(2026, 4, 10);
                List<WhoReceivedWhatResponse> mockLog = List.of(
                                WhoReceivedWhatResponse.builder()
                                                .deliveryDate(date).customerName("Alice Kumar")
                                                .customerAddress("12 Main St").publicationName("The Hindu")
                                                .status("DELIVERED").build(),
                                WhoReceivedWhatResponse.builder()
                                                .deliveryDate(date).customerName("Bob Singh")
                                                .customerAddress("45 Park Ave").publicationName("Times of India")
                                                .status("DELIVERED").build());

                when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<WhoReceivedWhatResponse>>any(),
                                any(LocalDate.class), any(LocalDate.class)))
                                .thenReturn(mockLog);

                // Act
                List<WhoReceivedWhatResponse> result = reportService.getWhoReceivedWhat(date, date);

                // Assert
                assertThat(result).hasSize(2);
                assertThat(result.get(0).getCustomerName()).isEqualTo("Alice Kumar");
                assertThat(result.get(0).getPublicationName()).isEqualTo("The Hindu");
                assertThat(result.get(0).getCustomerAddress()).isEqualTo("12 Main St");
                assertThat(result.get(0).getStatus()).isEqualTo("DELIVERED");
        }

        @Test
        @DisplayName("FR-RPT5-2: getWhoReceivedWhat should include CANCELLED deliveries in the log (not just DELIVERED)")
        void getWhoReceivedWhat_shouldIncludeAllDeliveryStatuses() {
                // Arrange — one DELIVERED, one CANCELLED
                LocalDate date = LocalDate.of(2026, 4, 10);
                List<WhoReceivedWhatResponse> mockLog = List.of(
                                WhoReceivedWhatResponse.builder()
                                                .deliveryDate(date).customerName("Alice Kumar")
                                                .publicationName("The Hindu").status("DELIVERED").build(),
                                WhoReceivedWhatResponse.builder()
                                                .deliveryDate(date).customerName("Charlie Doe")
                                                .publicationName("Deccan Herald").status("CANCELLED").build());

                when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<WhoReceivedWhatResponse>>any(),
                                any(LocalDate.class), any(LocalDate.class)))
                                .thenReturn(mockLog);

                // Act
                List<WhoReceivedWhatResponse> result = reportService.getWhoReceivedWhat(date, date);

                // Assert — both statuses present; report covers ALL deliveries not just
                // successful ones
                assertThat(result).hasSize(2);
                assertThat(result).extracting(WhoReceivedWhatResponse::getStatus)
                                .containsExactlyInAnyOrder("DELIVERED", "CANCELLED");
        }

        @Test
        @DisplayName("FR-RPT5-3: getWhoReceivedWhat for a date range with no deliveries should return empty list")
        void getWhoReceivedWhat_whenNoDeliveries_shouldReturnEmptyList() {
                // Arrange
                when(jdbcTemplate.query(anyString(), ArgumentMatchers.<RowMapper<WhoReceivedWhatResponse>>any(),
                                any(LocalDate.class), any(LocalDate.class)))
                                .thenReturn(List.of());

                // Act
                List<WhoReceivedWhatResponse> result = reportService.getWhoReceivedWhat(
                                LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 1));

                // Assert
                assertThat(result).isEmpty();
        }
}
