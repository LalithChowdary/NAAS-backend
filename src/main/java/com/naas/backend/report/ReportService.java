package com.naas.backend.report;

import com.naas.backend.report.dto.*;
import com.naas.backend.delivery.repository.DeliveryRecordRepository;
import com.naas.backend.delivery.entity.DeliveryRecord;
import com.naas.backend.subscription.SubscriptionRepository;
import com.naas.backend.subscription.Subscription;
import com.naas.backend.subscription.SubscriptionItem;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.UUID;

@Service
public class ReportService {

        private final JdbcTemplate jdbcTemplate;
        private final DeliveryRecordRepository deliveryRecordRepository;
        private final SubscriptionRepository subscriptionRepository;

        public ReportService(JdbcTemplate jdbcTemplate, DeliveryRecordRepository deliveryRecordRepository,
                        SubscriptionRepository subscriptionRepository) {
                this.jdbcTemplate = jdbcTemplate;
                this.deliveryRecordRepository = deliveryRecordRepository;
                this.subscriptionRepository = subscriptionRepository;
        }

        public MonthlySummaryResponse getMonthlySummary(String month) {
                String subSql = "SELECT COUNT(*) FROM subscriptions WHERE status = 'ACTIVE'";
                Long activeSubs = jdbcTemplate.queryForObject(subSql, Long.class);

                String billSql = "SELECT SUM(total_amount) FROM bills WHERE billing_month = ?";
                BigDecimal totalBilled = jdbcTemplate.queryForObject(billSql, BigDecimal.class, month);

                String[] parts = month.split("-");
                int mm = Integer.parseInt(parts[0]);
                int yyyy = Integer.parseInt(parts[1]);
                LocalDate start = LocalDate.of(yyyy, mm, 1);
                LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

                String paySql = "SELECT SUM(amount) FROM payments WHERE paid_at >= ? AND paid_at < ?";
                BigDecimal totalCollected = jdbcTemplate.queryForObject(paySql, BigDecimal.class, start.atStartOfDay(),
                                end.plusDays(1).atStartOfDay());

                String delTotalSql = "SELECT COUNT(*) FROM delivery_records WHERE delivery_date >= ? AND delivery_date <= ?";
                Long totalDel = jdbcTemplate.queryForObject(delTotalSql, Long.class, start, end);

                String delSuccSql = "SELECT COUNT(*) FROM delivery_records WHERE delivery_date >= ? AND delivery_date <= ? AND status = 'DELIVERED'";
                Long totalSucc = jdbcTemplate.queryForObject(delSuccSql, Long.class, start, end);

                return MonthlySummaryResponse.builder()
                                .month(month)
                                .totalActiveSubscriptions(activeSubs == null ? 0 : activeSubs)
                                .totalBilled(totalBilled == null ? BigDecimal.ZERO : totalBilled)
                                .totalCollected(totalCollected == null ? BigDecimal.ZERO : totalCollected)
                                .totalDeliveries(totalDel == null ? 0 : totalDel)
                                .successfulDeliveries(totalSucc == null ? 0 : totalSucc)
                                .build();
        }

        public List<OutstandingDuesResponse> getOutstandingDues() {
                String sql = "SELECT c.name as customerName, c.phone as phoneNumber, u.email as email, " +
                                "SUM(b.total_amount) as totalDue, COUNT(b.id) as pendingBillsCount " +
                                "FROM bills b " +
                                "JOIN customers c ON b.customer_id = c.id " +
                                "JOIN users u ON c.user_id = u.id " +
                                "WHERE b.status = 'UNPAID' " +
                                "GROUP BY c.id, c.name, c.phone, u.email " +
                                "ORDER BY totalDue DESC";

                return jdbcTemplate.query(sql, (rs, rowNum) -> OutstandingDuesResponse.builder()
                                .customerName(rs.getString("customerName"))
                                .phoneNumber(rs.getString("phoneNumber"))
                                .email(rs.getString("email"))
                                .totalDue(rs.getBigDecimal("totalDue"))
                                .pendingBillsCount(rs.getInt("pendingBillsCount"))
                                .build());
        }

        public List<DeliverySummaryResponse> getDeliverySummary(LocalDate startDate, LocalDate endDate) {
                String sql = "SELECT delivery_date, " +
                                "COUNT(id) as totalAssigned, " +
                                "SUM(CASE WHEN status = 'DELIVERED' THEN 1 ELSE 0 END) as delivered, " +
                                "SUM(CASE WHEN status = 'CANCELLED' THEN 1 ELSE 0 END) as cancelled, " +
                                "SUM(CASE WHEN status = 'PENDING' THEN 1 ELSE 0 END) as pending " +
                                "FROM delivery_records " +
                                "WHERE delivery_date BETWEEN ? AND ? " +
                                "GROUP BY delivery_date " +
                                "ORDER BY delivery_date ASC";

                return jdbcTemplate.query(sql, (rs, rowNum) -> DeliverySummaryResponse.builder()
                                .date(rs.getDate("delivery_date").toLocalDate())
                                .totalAssigned(rs.getLong("totalAssigned"))
                                .delivered(rs.getLong("delivered"))
                                .cancelled(rs.getLong("cancelled"))
                                .pending(rs.getLong("pending"))
                                .build(),
                                startDate, endDate);
        }

        public List<DeliveryPersonnelPaymentResponse> getDeliveryPersonnelPayment(LocalDate startDate,
                        LocalDate endDate) {
                String sql = "SELECT dp.id as deliveryPersonId, dp.name as deliveryPersonName, dp.employee_id as employeeId, "
                                +
                                "COUNT(DISTINCT d.id) as deliveriesCompleted, " +
                                "(SELECT COALESCE(SUM(amount_paid), 0) FROM delivery_person_payouts dpp WHERE dpp.delivery_person_id = dp.id AND ((dpp.start_date >= ? AND dpp.start_date <= ?) OR (dpp.end_date >= ? AND dpp.end_date <= ?) OR (dpp.start_date <= ? AND dpp.end_date >= ?))) as alreadyPaid "
                                +
                                "FROM delivery_records d " +
                                "JOIN delivery_persons dp ON d.delivery_person_id = dp.id " +
                                "WHERE d.status = 'DELIVERED' AND d.delivery_date BETWEEN ? AND ? " +
                                "GROUP BY dp.id, dp.name, dp.employee_id " +
                                "ORDER BY deliveriesCompleted DESC";

                List<DeliveryPersonnelPaymentResponse> responses = new java.util.ArrayList<>(
                                jdbcTemplate.query(sql, (rs, rowNum) -> {
                                        BigDecimal alreadyPaid = rs.getBigDecimal("alreadyPaid");
                                        if (alreadyPaid == null)
                                                alreadyPaid = BigDecimal.ZERO;

                                        return DeliveryPersonnelPaymentResponse.builder()
                                                        .deliveryPersonId(UUID
                                                                        .fromString(rs.getString("deliveryPersonId")))
                                                        .deliveryPersonName(rs.getString("deliveryPersonName"))
                                                        .employeeId(rs.getString("employeeId"))
                                                        .deliveriesCompleted(rs.getLong("deliveriesCompleted"))
                                                        .alreadyPaid(alreadyPaid)
                                                        .build();
                                }, startDate, endDate, startDate, endDate, startDate, endDate, startDate, endDate));

                for (DeliveryPersonnelPaymentResponse res : responses) {
                        List<DeliveryRecord> records = deliveryRecordRepository
                                        .findByDeliveryPersonIdAndDeliveryDateBetweenAndStatus(
                                                        res.getDeliveryPersonId(), startDate, endDate,
                                                        DeliveryRecord.DeliveryStatus.DELIVERED);

                        double total = 0.0;
                        for (DeliveryRecord rec : records) {
                                Subscription sub = subscriptionRepository.findById(rec.getSubscriptionId())
                                                .orElse(null);
                                if (sub != null && sub.getItems() != null) {
                                        total += sub.getItems().stream()
                                                        .filter(i -> i.isActiveOn(rec.getDeliveryDate()))
                                                        .mapToDouble(i -> i.getPublication().getPrice().doubleValue())
                                                        .sum();
                                }
                        }

                        BigDecimal totalVal = BigDecimal.valueOf(total);
                        BigDecimal payout = totalVal.multiply(new BigDecimal("0.025"));

                        BigDecimal remainingPayout = payout.subtract(res.getAlreadyPaid());
                        if (remainingPayout.compareTo(BigDecimal.ZERO) < 0) {
                                remainingPayout = BigDecimal.ZERO;
                        }

                        res.setTotalDeliveryValue(totalVal);
                        res.setPaymentAmount(payout);
                        res.setRemainingPayout(remainingPayout);
                }

                responses.sort((a, b) -> b.getTotalDeliveryValue().compareTo(a.getTotalDeliveryValue()));
                return responses;
        }

        public List<WhoReceivedWhatResponse> getWhoReceivedWhat(LocalDate startDate, LocalDate endDate) {
                String sql = "SELECT d.delivery_date as deliveryDate, c.name as customerName, " +
                                "ca.address as address, p.name as publicationName, d.status as status " +
                                "FROM delivery_records d " +
                                "JOIN customers c ON d.customer_id = c.id " +
                                "LEFT JOIN subscriptions s ON d.subscription_id = s.id " +
                                "LEFT JOIN customer_addresses ca ON s.customer_address_id = ca.id " +
                                "LEFT JOIN publications p ON d.publication_id = p.id " +
                                "WHERE d.delivery_date BETWEEN ? AND ? " +
                                "ORDER BY d.delivery_date ASC, c.name ASC";

                return jdbcTemplate.query(sql, (rs, rowNum) -> WhoReceivedWhatResponse.builder()
                                .deliveryDate(rs.getDate("deliveryDate").toLocalDate())
                                .customerName(rs.getString("customerName"))
                                .customerAddress(rs.getString("address"))
                                .publicationName(rs.getString("publicationName"))
                                .status(rs.getString("status"))
                                .build(),
                                startDate, endDate);
        }
}
