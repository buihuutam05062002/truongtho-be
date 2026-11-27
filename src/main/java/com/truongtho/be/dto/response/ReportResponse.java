package com.truongtho.be.dto.response;

import com.truongtho.be.enums.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ReportResponse(
        LocalDate from,
        LocalDate to,
        Summary summary,
        List<DailyPoint> daily,
        List<ProductRow> topProducts,
        List<EmployeeRow> byEmployee,
        List<StatusRow> reservations) {

    public record Summary(BigDecimal revenue, long paidOrders, BigDecimal averageOrderValue, long totalReservations) {
    }

    public record DailyPoint(LocalDate date, BigDecimal revenue, long orders) {
    }

    public record ProductRow(Long productId, String name, long quantity, BigDecimal revenue) {
    }

    public record EmployeeRow(String employeeId, String fullName, long orders, BigDecimal revenue) {
    }

    public record StatusRow(ReservationStatus status, long count) {
    }
}
