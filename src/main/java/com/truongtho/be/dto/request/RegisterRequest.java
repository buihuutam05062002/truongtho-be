package com.truongtho.be.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {
//    @NotBlank
//    @Size(min = 4, max = 30)
//    String username;
//    @NotBlank
//    String fullName;
    @NotBlank
    @Email
    String email;
    @NotBlank
    @Size(min = 8)
    String password;
//    String phoneNumber;
}