package com.truongtho.be.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CreateEmployeeRequest {
    @NotBlank
    @Pattern(regexp = "^[a-zA-Z0-9_.]{4,30}$")
    String username;
    @NotBlank
    String fullName;
    @NotBlank
    @Email
    String email;
    String phoneNumber;
    @NotNull
    LocalDate hireDate;
}