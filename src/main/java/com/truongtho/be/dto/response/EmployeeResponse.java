package com.truongtho.be.dto.response;

import com.truongtho.be.enums.EmployeeStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class EmployeeResponse {
    String id;
    String username;
    String fullName;
    String email;
    String phoneNumber;
    String avatarUrl;
    LocalDate hireDate;
    EmployeeStatus employeeStatus;
}
