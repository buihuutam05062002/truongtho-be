package com.truongtho.be.dto.response;

import com.truongtho.be.enums.ReservationStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReservationResponse {
    Long id;
    String reservationCode;
    String contactName;
    String contactPhone;
    LocalDateTime reservationTime;
    Integer partySize;
    String note;
    ReservationStatus reservationStatus;
    LocalDateTime createdAt;
}
