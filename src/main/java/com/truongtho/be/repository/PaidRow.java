package com.truongtho.be.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface PaidRow {
    LocalDateTime getPaidAt();

    BigDecimal getAmount();

    String getInvoiceId();
}
