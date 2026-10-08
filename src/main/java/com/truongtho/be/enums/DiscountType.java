package com.truongtho.be.enums;

public enum DiscountType {
    FIXED_AMOUNT("Số tiền"),
    PERCENTAGE("Phần trăm");

    private String description;

    DiscountType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
