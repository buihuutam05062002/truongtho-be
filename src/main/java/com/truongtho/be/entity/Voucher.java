package com.truongtho.be.entity;

import com.truongtho.be.enums.DiscountType;
import com.truongtho.be.enums.VoucherStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Voucher {
    @Id
            @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    @Enumerated(EnumType.STRING)
    DiscountType discountType;
    @Enumerated(EnumType.STRING)
    VoucherStatus voucherStatus;

    @OneToMany(mappedBy = "voucher")
    Set<Invoice> invoices;
}
