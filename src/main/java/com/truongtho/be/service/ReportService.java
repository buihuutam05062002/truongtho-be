package com.truongtho.be.service;

import com.truongtho.be.dto.response.ReportResponse;
import com.truongtho.be.dto.response.ReportResponse.DailyPoint;
import com.truongtho.be.dto.response.ReportResponse.EmployeeRow;
import com.truongtho.be.dto.response.ReportResponse.ProductRow;
import com.truongtho.be.dto.response.ReportResponse.StatusRow;
import com.truongtho.be.dto.response.ReportResponse.Summary;
import com.truongtho.be.enums.ReservationStatus;
import com.truongtho.be.exception.AppException;
import com.truongtho.be.repository.InvoiceDetailRepository;
import com.truongtho.be.repository.InvoiceRepository;
import com.truongtho.be.repository.PaidRow;
import com.truongtho.be.repository.PaymentRepository;
import com.truongtho.be.repository.ReservationRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReportService {

    static final int MAX_DAYS = 366;
    static final int TOP_PRODUCTS = 10;

    final PaymentRepository paymentRepository;
    final InvoiceRepository invoiceRepository;
    final InvoiceDetailRepository invoiceDetailRepository;
    final ReservationRepository reservationRepository;

    @Transactional(readOnly = true)
    public ReportResponse getReport(LocalDate from, LocalDate to) {
        LocalDate end = to != null ? to : LocalDate.now();
        LocalDate begin = from != null ? from : end.minusDays(29);
        if (begin.isAfter(end)) throw new AppException("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc");
        if (ChronoUnit.DAYS.between(begin, end) >= MAX_DAYS)
            throw new AppException("Khoảng thời gian tối đa là " + MAX_DAYS + " ngày");

        LocalDateTime start = begin.atStartOfDay();
        LocalDateTime stop = end.plusDays(1).atStartOfDay();

        Map<LocalDate, BigDecimal> revenueByDay = new TreeMap<>();
        Map<LocalDate, Set<String>> ordersByDay = new HashMap<>();
        for (LocalDate d = begin; !d.isAfter(end); d = d.plusDays(1)) revenueByDay.put(d, BigDecimal.ZERO);

        BigDecimal revenue = BigDecimal.ZERO;
        Set<String> paidInvoices = new HashSet<>();
        for (PaidRow row : paymentRepository.findPaidBetween(start, stop)) {
            BigDecimal amount = row.getAmount() != null ? row.getAmount() : BigDecimal.ZERO;
            LocalDate day = row.getPaidAt().toLocalDate();
            revenueByDay.merge(day, amount, BigDecimal::add);
            ordersByDay.computeIfAbsent(day, k -> new HashSet<>()).add(row.getInvoiceId());
            revenue = revenue.add(amount);
            paidInvoices.add(row.getInvoiceId());
        }
        List<DailyPoint> daily = revenueByDay.entrySet().stream()
                .map(e -> new DailyPoint(e.getKey(), e.getValue(),
                        ordersByDay.getOrDefault(e.getKey(), Set.of()).size()))
                .toList();

        long paidOrders = paidInvoices.size();
        BigDecimal average = paidOrders > 0
                ? revenue.divide(BigDecimal.valueOf(paidOrders), 0, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<ProductRow> topProducts = invoiceDetailRepository
                .topProducts(start, stop, PageRequest.of(0, TOP_PRODUCTS)).stream()
                .map(r -> new ProductRow((Long) r[0], (String) r[1],
                        ((Number) r[2]).longValue(), (BigDecimal) r[3]))
                .toList();

        List<EmployeeRow> byEmployee = invoiceRepository.revenueByEmployee(start, stop).stream()
                .map(r -> new EmployeeRow((String) r[0],
                        r[1] != null ? (String) r[1] : "Không xác định",
                        ((Number) r[2]).longValue(),
                        r[3] != null ? (BigDecimal) r[3] : BigDecimal.ZERO))
                .toList();

        Map<ReservationStatus, Long> byStatus = new EnumMap<>(ReservationStatus.class);
        for (Object[] r : reservationRepository.countByStatusBetween(start, stop)) {
            if (r[0] != null) byStatus.put((ReservationStatus) r[0], (Long) r[1]);
        }
        List<StatusRow> reservations = Arrays.stream(ReservationStatus.values())
                .map(s -> new StatusRow(s, byStatus.getOrDefault(s, 0L)))
                .toList();
        long totalReservations = byStatus.values().stream().mapToLong(Long::longValue).sum();

        return new ReportResponse(begin, end,
                new Summary(revenue, paidOrders, average, totalReservations),
                daily, topProducts, byEmployee, reservations);
    }
}
