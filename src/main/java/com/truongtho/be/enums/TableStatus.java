package com.truongtho.be.enums;


public enum TableStatus {

    AVAILABLE ("Trống"),
    MAINTENANCE ("Đang bảo trì"),
    IN_USE("Đang sử dụng"),
    RESERVED("Đã đặt trước"),
    SERVING("Đang phục vụ");

    private String description;

    TableStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
