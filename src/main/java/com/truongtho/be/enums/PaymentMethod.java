package com.truongtho.be.enums;

public enum PaymentMethod {
    ONLINE ("Trực tuyến"),
    CASH ("Tiền mặt"),
    BANK_TRANSFER ("Chuyển khoản");

    private String description;

    PaymentMethod(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
