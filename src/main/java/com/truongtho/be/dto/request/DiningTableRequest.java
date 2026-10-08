package com.truongtho.be.dto.request;

import com.truongtho.be.enums.TableStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DiningTableRequest {
    String name;
    Integer position;
    Integer capacity;
    TableStatus tableStatus;
}
