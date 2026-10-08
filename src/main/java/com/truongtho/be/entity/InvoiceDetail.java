package com.truongtho.be.entity;

import jakarta.persistence.*;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Entity
@Table(name = "invoice_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InvoiceDetail {
    @EmbeddedId
    InvoiceDetailId invoiceDetailId;

    Integer quantity;
    BigDecimal unitPrice;
    BigDecimal total;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("productId")
            @JoinColumn(name = "product_id")
    Product product;

    @ManyToOne
    @MapsId("invoiceId")
    @JoinColumn(name = "invoice_id")
    Invoice invoice;

    @EqualsAndHashCode.Include
    Long productKey() {
        return product != null ? product.getId() : null;
    }

}
