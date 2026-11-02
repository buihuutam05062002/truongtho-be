package com.truongtho.be.dto.request;

import com.truongtho.be.enums.ReservationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReservationStatusRequest {
    @NotNull(message = "Vui lòng chọn trạng thái")
    ReservationStatus reservationStatus;
}
