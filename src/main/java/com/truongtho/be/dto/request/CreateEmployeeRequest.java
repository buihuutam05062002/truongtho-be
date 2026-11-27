package com.truongtho.be.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CreateEmployeeRequest {
    @NotBlank
    String fullName;
    @NotBlank
    @Email
    String email;
    String phoneNumber;
    @NotNull
    LocalDate hireDate;
}