package com.truongtho.be.enums;

public enum EmployeeStatus {
    ACTIVE ("Hoạt động"),
    RESIGNED ("Đã nghỉ việc"),
    ON_LEAVE ("Nghỉ phép"),
    SUSPENDED("Ngưng");

    private String description ;

    EmployeeStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
