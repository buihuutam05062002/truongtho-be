package com.truongtho.be.dto.response;

import com.truongtho.be.enums.PaymentMethod;
import com.truongtho.be.enums.PaymentStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentResponse {
    String id;
    String invoiceId;

    BigDecimal amount;
    String transactionCode;

    PaymentMethod paymentMethod;
    PaymentStatus paymentStatus;

    LocalDateTime createdAt;
    LocalDateTime paidAt;


    String QrUrl;
}
