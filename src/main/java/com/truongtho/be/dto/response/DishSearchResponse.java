package com.truongtho.be.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DishSearchResponse {
    Long id;
    String name;
    String description;
    BigDecimal unitPrice;
    String priceText;
    String categoryName;
}