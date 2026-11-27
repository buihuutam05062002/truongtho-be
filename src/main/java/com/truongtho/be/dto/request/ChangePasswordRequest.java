package com.truongtho.be.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePasswordRequest {
    @NotBlank
    String currentPassword;
    @NotBlank
    @Size(min = 8, max = 72)
    String newPassword;
}
