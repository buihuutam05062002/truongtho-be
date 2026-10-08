package com.truongtho.be.enums;

public enum UserStatus {
    ACTIVE ("Hoạt động"),
    INACTIVE ("Không hoạt động"),
    LOCKED ("Khóa"),
    PENDING ("Chờ"),
    DELETED ("Xóa");


    private String description;

    UserStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
