package com.truongtho.be.enums;

public enum ReservationStatus {
    PENDING ("Chờ"),
    CONFIRM ("Chấp nhận"),
    ARRIVED ("Đã đến"),
    COMPLETED ("Đã hoàn thành"),
    CANCELLED ("Đã hủy"),
    NO_SHOW ("Không đến");


    private String description;


    ReservationStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
