package com.truongtho.be.dto.request;

import com.truongtho.be.entity.Invoice;
import com.truongtho.be.enums.DiscountType;
import com.truongtho.be.enums.VoucherStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VoucherRequest {
    String code;
    String description;

    BigDecimal discountValue;
    BigDecimal discountValueMax;
    BigDecimal minimumOrderAmount;

    Integer maxQuantity;
    Integer usedQuantity;

    LocalDateTime start;
    LocalDateTime expire;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    DiscountType discountType;

    VoucherStatus voucherStatus;
}
