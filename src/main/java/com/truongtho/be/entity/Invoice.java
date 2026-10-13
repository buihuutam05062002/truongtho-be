package com.truongtho.be.entity;

import com.truongtho.be.enums.InvoiceStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@jakarta.persistence.Table(name = "invoices")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Invoice {
    @Id
    @Column(name = "invoice_id")
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    LocalDateTime issuedAt;

    BigDecimal discountAmount;
    BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    InvoiceStatus invoiceStatus;


    @ManyToOne
    @JoinColumn(name = "voucher_id")
    Voucher voucher;


    @ManyToOne
    @JoinColumn(name = "table_id")
    DiningTable diningTable;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", unique = true)
    Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY,optional = true)
    @JoinColumn(name = "createdBy")
    Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    Customer customer;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL,orphanRemoval = true)
    Set<InvoiceDetail> invoiceDetails  = new HashSet<>();;
}
