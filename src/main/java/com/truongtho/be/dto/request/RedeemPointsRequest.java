package com.truongtho.be.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RedeemPointsRequest {
    /** 0 = remove the points discount. */
    @NotNull(message = "Vui lòng nhập số điểm")
    @Min(value = 0, message = "Số điểm không hợp lệ")
    Integer points;
}
