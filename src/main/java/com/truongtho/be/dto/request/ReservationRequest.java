package com.truongtho.be.dto.request;

import com.truongtho.be.entity.Customer;
import com.truongtho.be.entity.DiningTable;
import com.truongtho.be.entity.Employee;
import com.truongtho.be.enums.ReservationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReservationRequest {
    String contactName;
    String contactPhone;
    LocalDateTime reservationTime;
    Integer partySize;
    String note;
    ReservationStatus reservationStatus;
    String confirmBy;
    String diningTable;
    String customer;
}
