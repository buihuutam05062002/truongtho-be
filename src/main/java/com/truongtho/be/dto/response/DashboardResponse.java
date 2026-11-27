package com.truongtho.be.dto.response;

import com.truongtho.be.enums.InvoiceStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DashboardResponse {
    LocalDate date;
    LocalDateTime generatedAt;
    Kpi kpi;
    TableOverview tables;
    List<HourlyPoint> hourly;
    List<DailyPoint> daily;
    List<RecentOrder> recentOrders;


    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Kpi {
        BigDecimal revenue;
        Double revenueChangePercent;
        long totalOrders;
        long paidOrders;
        long openOrders;
        long guests;
        BigDecimal averageOrderValue;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class TableOverview {
        long total;
        long occupied;
        long available;
        long reserved;
        long maintenance;
        int occupancyPercent;
        long upcomingReservations;
        long upcomingGuests;
    }


    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class HourlyPoint {
        int hour;
        BigDecimal today;
        BigDecimal yesterday;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class DailyPoint {
        LocalDate date;
        BigDecimal revenue;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class RecentOrder {
        String id;
        String tableName;
        String employeeName;
        BigDecimal totalAmount;
        InvoiceStatus invoiceStatus;
        LocalDateTime issuedAt;
    }
}