package com.truongtho.be.dto.request;

import com.truongtho.be.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentRequest {
    @NotNull(message = "Phương thức thanh toán không được để trống")
    PaymentMethod paymentMethod;
}
