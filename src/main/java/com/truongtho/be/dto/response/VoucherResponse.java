package com.truongtho.be.dto.response;

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
@FieldDefaults(level =  AccessLevel.PRIVATE)
public class VoucherResponse {

    Long id;
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
