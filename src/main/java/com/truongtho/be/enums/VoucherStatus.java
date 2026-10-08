package com.truongtho.be.enums;

public enum VoucherStatus {
    DRAFT ("Nháp"),
    ACTIVE ("Hoạt động"),
    SUSPENDED ("Tạm ngưng"),
    INACTIVE ("Không hoạt động");

    private String description;

    VoucherStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
