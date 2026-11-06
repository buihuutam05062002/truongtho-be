package com.truongtho.be.dto.request;

import com.truongtho.be.enums.TableStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DiningTableRequest {
    @NotBlank(message = "Tên không được trống")
    String name;
    Integer position;
    @Min(value = 1, message = "Giá trị không được nhỏ hơn 1")
            @NotNull(message = "Không được rỗng")
    Integer capacity;
    @NotNull(message = "Không được rỗng")
    TableStatus tableStatus;
}
