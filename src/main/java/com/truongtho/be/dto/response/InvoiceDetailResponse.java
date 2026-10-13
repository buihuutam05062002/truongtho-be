package com.truongtho.be.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InvoiceDetailResponse {
    Long productId;
    String productName;
    Integer quantity;
    BigDecimal unitPrice;
    BigDecimal total;
}
