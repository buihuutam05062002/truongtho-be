package com.truongtho.be.dto.request;

import com.truongtho.be.enums.EmployeeStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UpdateEmployeeRequest {
    @NotBlank
    String fullName;
    String phoneNumber;
    @NotNull
    LocalDate hireDate;
    @NotNull
    EmployeeStatus employeeStatus;
}
