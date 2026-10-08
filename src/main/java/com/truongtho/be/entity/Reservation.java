package com.truongtho.be.entity;

import com.truongtho.be.enums.ReservationStatus;
import jakarta.persistence.*;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "reservations")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservation_id")
    Long id;

    @Column(name = "reservation_code")
    String reservationCode;

    String contactName;
    String contactPhone;

    LocalDateTime reservationTime;
    Integer partySize;
    String note;

    @Enumerated(EnumType.STRING)
    ReservationStatus reservationStatus;

    @CreationTimestamp
    LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by")
    Employee confirmBy;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_id")
    DiningTable diningTable;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    Customer customer;
}
