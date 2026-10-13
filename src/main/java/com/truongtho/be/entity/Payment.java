package com.truongtho.be.entity;

import com.truongtho.be.enums.PaymentMethod;
import com.truongtho.be.enums.PaymentStatus;
import jakarta.persistence.*;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "payments")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    BigDecimal amount;

    @Column(unique = true, nullable = false)
    String transactionCode;

    @Enumerated(EnumType.STRING)
    PaymentStatus paymentStatus;

    @Enumerated(EnumType.STRING)
    PaymentMethod paymentMethod;

    LocalDateTime createdAt;
    LocalDateTime paidAt;

    @Column(unique = true)
    Long sepayTransactionId;



    @Column(unique = true)
    String gatewayTransactionId;

    @ManyToOne(fetch = FetchType.LAZY)
            @JoinColumn(name = "employee_id")
    Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
            @JoinColumn(name = "invoice_id")
    Invoice invoice;
}
