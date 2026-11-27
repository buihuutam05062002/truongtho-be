package com.truongtho.be.dto.response;

import com.truongtho.be.enums.EmployeeStatus;
import com.truongtho.be.enums.UserStatus;

import java.util.Set;

public record AdminUserResponse(
        String id,
        String fullName,
        String email,
        String phoneNumber,
        String avatarUrl,
        UserStatus userStatus,
        Set<String> roles,
        EmployeeStatus employeeStatus) {
}
