package com.truongtho.be.dto.request;

import com.truongtho.be.entity.Customer;
import com.truongtho.be.entity.DiningTable;
import com.truongtho.be.entity.Employee;
import com.truongtho.be.enums.ReservationStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
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
    @NotBlank(message = "Vui lòng nhập họ và tên")
            @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
    String contactName;
    @NotBlank(message = "Vui lòng nhập số điện thoại")
            @Pattern(regexp = "^(0|\\+84)\\d{9}$", message = "số điện thoại không hợp lệ")
    String contactPhone;

    @NotNull(message = "Vui lòng chọn thời gian")
    @Future(message = "thời gian đặt bàn phải ở tương lai")
    LocalDateTime reservationTime;

    @NotNull(message = "Vui lòng nhập số nguoười")
            @Min(value = 1, message = "Tối thiểu 1 người")
            @Max(value = 50, message = "Tối đa 50 người, vui lòng liên hệ nhà hàng")
    Integer partySize;

    @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
    String note;
}
