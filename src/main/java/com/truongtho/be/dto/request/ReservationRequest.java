package com.truongtho.be.dto.request;

import com.truongtho.be.entity.Customer;
import com.truongtho.be.entity.DiningTable;
import com.truongtho.be.entity.Employee;
import com.truongtho.be.enums.ReservationStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    @NotBlank
    String contactName;
    @NotBlank
    String contactPhone;

    @NotNull
    @Future
    LocalDateTime reservationTime;

    @NotNull
            @Min(1)
    Integer partySize;
    String note;
}
