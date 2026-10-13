package com.truongtho.be.dto.response;

import com.truongtho.be.enums.InvoiceStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InvoiceResponse {
    String id;
    String tableName;
    String employeeName;

    BigDecimal subTotal;
    BigDecimal discountAmount;
    BigDecimal totalAmount;

    InvoiceStatus invoiceStatus;
    String voucherCode;
    LocalDateTime issuedAt;

    List<InvoiceDetailResponse> details;
}
