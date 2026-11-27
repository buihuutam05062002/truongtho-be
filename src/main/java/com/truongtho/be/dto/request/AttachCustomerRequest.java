package com.truongtho.be.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AttachCustomerRequest {
    @NotBlank(message = "Vui lòng nhập email hoặc số điện thoại")
    String keyword;
}
