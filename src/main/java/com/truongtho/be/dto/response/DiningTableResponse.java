package com.truongtho.be.dto.response;

import com.truongtho.be.enums.TableStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level =  AccessLevel.PRIVATE)
public class DiningTableResponse {
    Long id;
    String name;
    Integer position;
    Integer capacity;
    TableStatus tableStatus;
}
