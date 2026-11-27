package com.truongtho.be.service;

import com.truongtho.be.dto.response.DashboardResponse;
import com.truongtho.be.dto.response.DashboardResponse.DailyPoint;
import com.truongtho.be.dto.response.DashboardResponse.HourlyPoint;
import com.truongtho.be.dto.response.DashboardResponse.Kpi;
import com.truongtho.be.dto.response.DashboardResponse.RecentOrder;
import com.truongtho.be.dto.response.DashboardResponse.TableOverview;
import com.truongtho.be.entity.Invoice;
import com.truongtho.be.enums.InvoiceStatus;
import com.truongtho.be.enums.ReservationStatus;
import com.truongtho.be.enums.TableStatus;
import com.truongtho.be.repository.DiningTableRepository;
import com.truongtho.be.repository.InvoiceRepository;
import com.truongtho.be.repository.PaymentRepository;
import com.truongtho.be.repository.PaidRow;
import com.truongtho.be.repository.ReservationRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DashboardService {

    final PaymentRepository paymentRepository;
    final InvoiceRepository invoiceRepository;
    final DiningTableRepository diningTableRepository;
    final ReservationRepository reservationRepository;

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(LocalDate date) {
        LocalDate today = LocalDate.now();
        LocalDate day = date != null ? date : today;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = day.atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        LocalDateTime yesterdayStart = start.minusDays(1);
        LocalDateTime weekStart = start.minusDays(6);
        LocalDateTime cutoff = day.equals(today) ? now : end;
        LocalDateTime yesterdayCutoff = cutoff.minusDays(1);

        BigDecimal[] todayByHour = zeros(24);
        BigDecimal[] yesterdayByHour = zeros(24);
        Map<LocalDate, BigDecimal> byDay = new TreeMap<>();
        for (int i = 0; i < 7; i++) byDay.put(weekStart.toLocalDate().plusDays(i), BigDecimal.ZERO);

        BigDecimal revenue = BigDecimal.ZERO;
        BigDecimal yesterdayRevenueToCutoff = BigDecimal.ZERO;
        Set<String> paidInvoiceIds = new HashSet<>();

        for (PaidRow row : paymentRepository.findPaidBetween(weekStart, end)) {
            LocalDateTime t = row.getPaidAt();
            BigDecimal amount = row.getAmount() != null ? row.getAmount() : BigDecimal.ZERO;

            byDay.merge(t.toLocalDate(), amount, BigDecimal::add);

            if (!t.isBefore(start)) {
                todayByHour[t.getHour()] = todayByHour[t.getHour()].add(amount);
                revenue = revenue.add(amount);
                paidInvoiceIds.add(row.getInvoiceId());
            } else if (!t.isBefore(yesterdayStart)) {
                yesterdayByHour[t.getHour()] = yesterdayByHour[t.getHour()].add(amount);
                if (t.isBefore(yesterdayCutoff)) {
                    yesterdayRevenueToCutoff = yesterdayRevenueToCutoff.add(amount);
                }
            }
        }

        Double changePercent = yesterdayRevenueToCutoff.signum() == 0
                ? null
                : revenue.subtract(yesterdayRevenueToCutoff)
                .multiply(BigDecimal.valueOf(100))
                .divide(yesterdayRevenueToCutoff, 1, RoundingMode.HALF_UP)
                .doubleValue();

        long paidOrders = paidInvoiceIds.size();
        BigDecimal average = paidOrders > 0
                ? revenue.divide(BigDecimal.valueOf(paidOrders), 0, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        long totalOrders = 0;
        for (Object[] r : invoiceRepository.countByStatusBetween(start, end)) {
            if (r[0] != InvoiceStatus.CANCELLED) totalOrders += (Long) r[1];
        }
        long openOrders = invoiceRepository.countByInvoiceStatus(InvoiceStatus.PENDING);

        long guests = reservationRepository.sumPartySize(
                List.of(ReservationStatus.ARRIVED, ReservationStatus.COMPLETED), start, end);

        Kpi kpi = Kpi.builder()
                .revenue(revenue)
                .revenueChangePercent(changePercent)
                .totalOrders(totalOrders)
                .paidOrders(paidOrders)
                .openOrders(openOrders)
                .guests(guests)
                .averageOrderValue(average)
                .build();

        Map<TableStatus, Long> byStatus = new EnumMap<>(TableStatus.class);
        for (Object[] r : diningTableRepository.countByStatus()) {
            if (r[0] != null) byStatus.put((TableStatus) r[0], (Long) r[1]);
        }
        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
        long occupied = byStatus.getOrDefault(TableStatus.IN_USE, 0L) + byStatus.getOrDefault(TableStatus.SERVING, 0L);
        long maintenance = byStatus.getOrDefault(TableStatus.MAINTENANCE, 0L);
        long usable = total - maintenance;
        int occupancy = usable > 0 ? (int) Math.round(occupied * 100.0 / usable) : 0;

        LocalDateTime upcomingFrom = day.equals(today) ? now : start;
        List<ReservationStatus> upcomingStatuses = List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED);
        TableOverview tables = TableOverview.builder()
                .total(total)
                .occupied(occupied)
                .available(byStatus.getOrDefault(TableStatus.AVAILABLE, 0L))
                .reserved(byStatus.getOrDefault(TableStatus.RESERVED, 0L))
                .maintenance(maintenance)
                .occupancyPercent(occupancy)
                .upcomingReservations(reservationRepository.countInRange(upcomingStatuses, upcomingFrom, end))
                .upcomingGuests(reservationRepository.sumPartySize(upcomingStatuses, upcomingFrom, end))
                .build();

        List<HourlyPoint> hourly = new ArrayList<>(24);
        for (int h = 0; h < 24; h++) {
            hourly.add(HourlyPoint.builder()
                    .hour(h)
                    .today(todayByHour[h])
                    .yesterday(yesterdayByHour[h])
                    .build());
        }

        List<DailyPoint> daily = byDay.entrySet().stream()
                .map(e -> DailyPoint.builder().date(e.getKey()).revenue(e.getValue()).build())
                .toList();

        List<RecentOrder> recent = invoiceRepository
                .findTop5ByIssuedAtGreaterThanEqualAndIssuedAtLessThanOrderByIssuedAtDesc(start, end)
                .stream()
                .map(this::toRecentOrder)
                .toList();

        return DashboardResponse.builder()
                .date(day)
                .generatedAt(now)
                .kpi(kpi)
                .tables(tables)
                .hourly(hourly)
                .daily(daily)
                .recentOrders(recent)
                .build();
    }

    private RecentOrder toRecentOrder(Invoice i) {
        String employeeName = i.getEmployee() != null && i.getEmployee().getUsers() != null
                ? i.getEmployee().getUsers().getFullName()
                : null;
        return RecentOrder.builder()
                .id(i.getId())
                .tableName(i.getDiningTable() != null ? i.getDiningTable().getName() : null)
                .employeeName(employeeName)
                .totalAmount(i.getTotalAmount())
                .invoiceStatus(i.getInvoiceStatus())
                .issuedAt(i.getIssuedAt())
                .build();
    }

    private static BigDecimal[] zeros(int n) {
        BigDecimal[] a = new BigDecimal[n];
        Arrays.fill(a, BigDecimal.ZERO);
        return a;
    }
}