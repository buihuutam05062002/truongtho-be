package com.truongtho.be.enums;

public enum InvoiceStatus {
    PENDING ("Chờ thanh toán"),
    PAID ("Đã thanh toán"),
    CANCELLED ("Đã tạm hoãn");


    private String description;


    InvoiceStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
