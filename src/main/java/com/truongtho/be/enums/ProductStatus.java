package com.truongtho.be.enums;

public enum ProductStatus {
    AVAILABLE("Có sẵn"),
    ON_SALE ("Đang bán"),
    OUT_OF_STOCK ("Hết hàng"),
    SUSPENDED ("Ngừng bán");

    private String description;

    ProductStatus(String description) {
        this.description = description;
    }


    public String getDescription() {
        return description;
    }
}
