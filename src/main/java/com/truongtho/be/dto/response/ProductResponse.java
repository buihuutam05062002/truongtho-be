package com.truongtho.be.dto.response;

import com.truongtho.be.enums.ProductStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class ProductResponse {
    Long id;
    String name;
    BigDecimal unitPrice;
    String description;
    ProductStatus productStatus;
    CategoryResponse categoryResponse;

    List<ProductImageResponse> productImageResponses;
}
