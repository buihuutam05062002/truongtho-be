package com.truongtho.be.enums;

public enum PaymentStatus {
    PENDING ("Chờ"),
    SUCCESS ("Thành công"),
    FAIL ("Thất bại"),
    CANCELLED ("Đã hủy");

    private String description;

    PaymentStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
